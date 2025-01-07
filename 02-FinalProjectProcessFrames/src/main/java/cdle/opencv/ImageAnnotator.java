package cdle.opencv;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfRect2d;
import org.opencv.core.Point;
import org.opencv.core.Rect2d;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;

public class ImageAnnotator {

    private Net net;                     // YOLO network for detection
    private List<String> classNames;     // List of class names for YOLO
    private String outputDir;            // Directory to save annotated images

    public ImageAnnotator(String modelConfigPath, String modelWeightsPath, String classNamesPath, String outputDir) throws Exception {
        // Load YOLO network
        this.net = Dnn.readNetFromDarknet(modelConfigPath, modelWeightsPath);
        this.net.setPreferableBackend(Dnn.DNN_BACKEND_OPENCV);
        this.net.setPreferableTarget(Dnn.DNN_TARGET_CPU);

        // Load class names
        this.classNames = Files.readAllLines(Paths.get(classNamesPath));

        // Set output directory for annotated images
        this.outputDir = outputDir;
    }

    public String annotateAndDetect(String imagePath, Mat image) {
        if (image.empty()) {
            return "Unknown Team";
        }

        try {
            // Perform YOLO detections
            List<Rect2d> playerBoxes = new ArrayList<>();
            List<Float> confidences = new ArrayList<>();
            getYoloDetections(image, playerBoxes, confidences);

            // Apply NMS to filter detections
            List<Rect2d> filteredPlayerBoxes = applyNMS(playerBoxes, confidences, 0.6f, 0.4f);

            // Annotate players and detect possession
            annotatePlayers(filteredPlayerBoxes, image);
            Rect2d basketballBox = detectBasketball(image, filteredPlayerBoxes);
            if (basketballBox != null) {
                annotateBasketball(basketballBox, image);
            }

            String possessingTeam = determinePossessingTeam(basketballBox, filteredPlayerBoxes, image);

            // Save annotated image
            saveAnnotatedImage(imagePath, image);

            return possessingTeam;

        } catch (Exception e) {
            e.printStackTrace();
            return "Unknown Team";
        }
    }

    private void getYoloDetections(Mat image, List<Rect2d> playerBoxes, List<Float> confidences) {
        Mat blob = null;
        List<Mat> outputs = new ArrayList<>();

        try {
            blob = Dnn.blobFromImage(image, 1 / 255.0, new Size(416, 416), new Scalar(0, 0, 0), true, false);
            this.net.setInput(blob);

            List<String> outputLayerNames = net.getUnconnectedOutLayersNames();
            net.forward(outputs, outputLayerNames);

            parseYoloResults(outputs, image, 0.5f, playerBoxes, confidences);
        } finally {
            if (blob != null) {
                blob.release();
            }
            for (Mat output : outputs) {
                output.release();
            }
        }
    }

    private void parseYoloResults(List<Mat> outputs, Mat image, float confThreshold, List<Rect2d> playerBoxes, List<Float> confidences) {
        for (Mat result : outputs) {
            for (int i = 0; i < result.rows(); i++) {
                Mat row = result.row(i);
                Mat scores = row.colRange(5, row.cols());
                Core.MinMaxLocResult mm = Core.minMaxLoc(scores);

                float confidence = (float) mm.maxVal;

                if (confidence > confThreshold) {
                    int centerX = (int) (row.get(0, 0)[0] * image.cols());
                    int centerY = (int) (row.get(0, 1)[0] * image.rows());
                    int width = (int) (row.get(0, 2)[0] * image.cols());
                    int height = (int) (row.get(0, 3)[0] * image.rows());
                    int x = centerX - width / 2;
                    int y = centerY - height / 2;

                    Rect2d box = new Rect2d(x, y, width, height);

                    int classId = (int) mm.maxLoc.x;
                    if (classNames.get(classId).equals("person")) {
                        playerBoxes.add(box);
                        confidences.add(confidence);
                    }
                }
            }
        }
    }

    private List<Rect2d> applyNMS(List<Rect2d> boxes, List<Float> confidences, float confThreshold, float nmsThreshold) {
        MatOfFloat confidencesMat = new MatOfFloat(Converters.vector_float_to_Mat(confidences));
        MatOfRect2d boxesMat = new MatOfRect2d(boxes.toArray(new Rect2d[0]));
        MatOfInt indices = new MatOfInt();

        Dnn.NMSBoxes(boxesMat, confidencesMat, confThreshold, nmsThreshold, indices);

        List<Rect2d> filteredBoxes = new ArrayList<>();
        for (int i = 0; i < indices.rows(); i++) {
            int idx = (int) indices.get(i, 0)[0];
            filteredBoxes.add(boxes.get(idx));
        }

        confidencesMat.release();
        boxesMat.release();
        indices.release();

        return filteredBoxes;
    }

    private Rect2d detectBasketball(Mat image, List<Rect2d> playerBoxes) {
        Mat hsvImage = new Mat();
        Mat maskOrange = new Mat();
        Mat circles = new Mat();

        try {
            Imgproc.cvtColor(image, hsvImage, Imgproc.COLOR_BGR2HSV);

            Core.inRange(hsvImage, new Scalar(5, 150, 150), new Scalar(15, 255, 255), maskOrange);

            Imgproc.GaussianBlur(maskOrange, maskOrange, new Size(9, 9), 2, 2);

            Imgproc.HoughCircles(maskOrange, circles, Imgproc.HOUGH_GRADIENT, 1, maskOrange.rows() / 8, 100, 20, 10, 50);

            if (circles.cols() > 0) {
                double minDistance = Double.MAX_VALUE;
                Rect2d closestBasketball = null;

                for (int i = 0; i < circles.cols(); i++) {
                    double[] circle = circles.get(0, i);
                    Point center = new Point(circle[0], circle[1]);
                    int radius = (int) circle[2];

                    Rect2d basketballBox = new Rect2d(center.x - radius, center.y - radius, radius * 2, radius * 2);

                    for (Rect2d playerBox : playerBoxes) {
                        Point playerCenter = new Point(playerBox.x + playerBox.width / 2.0, playerBox.y + playerBox.height / 2.0);
                        double distance = calculateDistance(center, playerCenter);

                        if (distance < minDistance) {
                            minDistance = distance;
                            closestBasketball = basketballBox;
                        }
                    }
                }

                return closestBasketball;
            }

            return null;
        } finally {
            hsvImage.release();
            maskOrange.release();
            circles.release();
        }
    }

    private void annotatePlayers(List<Rect2d> boxes, Mat image) {
        for (Rect2d box : boxes) {
            int x1 = Math.max((int) box.x, 0);
            int y1 = Math.max((int) box.y, 0);
            int x2 = Math.min((int) (box.x + box.width), image.cols());
            int y2 = Math.min((int) (box.y + box.height), image.rows());

            if (x1 >= x2 || y1 >= y2) {
                System.err.println("Invalid bounding box dimensions. Skipping annotation.");
                continue;
            }

            Mat shirtRegion = image.submat(y1, y2, x1, x2);
            String team = detectShirtColor(shirtRegion);
            Scalar color;

            if (team.contains("Lakers")) {
                color = new Scalar(0, 255, 255); 
            }else if (team.contains("Trailblazers")) {
                color = new Scalar(0, 0, 255); 
            }else {
                color = new Scalar(255, 0, 0);
            }

            Imgproc.rectangle(image, new Point(x1, y1), new Point(x2, y2), color, 2);
            Imgproc.putText(image, team, new Point(x1, y1 - 10), Imgproc.FONT_HERSHEY_TRIPLEX, 0.8, color, 2);

            shirtRegion.release();
        }
    }

    private void annotateBasketball(Rect2d basketballBox, Mat image) {
        if (basketballBox != null) {
            Imgproc.rectangle(image, basketballBox.tl(), basketballBox.br(), new Scalar(0, 165, 255), 2);
            Imgproc.putText(image, "Basketball", basketballBox.tl(), Imgproc.FONT_HERSHEY_SIMPLEX, 0.5, new Scalar(0, 165, 255), 2);
        }
    }

    private String detectShirtColor(Mat region) {
        Mat hsvRegion = new Mat();
        Imgproc.cvtColor(region, hsvRegion, Imgproc.COLOR_BGR2HSV);

        List<Mat> hsvChannels = new ArrayList<>();
        Core.split(hsvRegion, hsvChannels);
        Imgproc.equalizeHist(hsvChannels.get(2), hsvChannels.get(2));
        Core.merge(hsvChannels, hsvRegion);
        for (Mat channel : hsvChannels) {
            channel.release();
        }

        Mat maskRed = new Mat();
        Mat maskYellow = new Mat();

        try {
            Mat maskRedLower = new Mat();
            Mat maskRedUpper = new Mat();
            Core.inRange(hsvRegion, new Scalar(0, 150, 150), new Scalar(10, 255, 255), maskRedLower);
            Core.inRange(hsvRegion, new Scalar(160, 150, 150), new Scalar(180, 255, 255), maskRedUpper);
            Core.addWeighted(maskRedLower, 1.0, maskRedUpper, 1.0, 0.0, maskRed);
            maskRedLower.release();
            maskRedUpper.release();

            Core.inRange(hsvRegion, new Scalar(20, 150, 150), new Scalar(30, 255, 255), maskYellow);

            int redPixels = Core.countNonZero(maskRed);
            int yellowPixels = Core.countNonZero(maskYellow);

            int margin = 100;

            if (redPixels > yellowPixels + margin) {
                return "Trailblazers (Red)";
            } else if (yellowPixels > redPixels + margin) {
                return "Lakers (Yellow)";
            } else {
                return "Unknown Team";
            }
        } finally {
            hsvRegion.release();
            maskRed.release();
            maskYellow.release();
        }
    }

    private String determinePossessingTeam(Rect2d basketballBox, List<Rect2d> playerBoxes, Mat image) {
        if (basketballBox == null) {
            return "Unknown Team";
        }

        double minDistance = Double.MAX_VALUE;
        String possessingTeam = "Unknown Team";

        Point ballCenter = new Point(
                basketballBox.x + basketballBox.width / 2.0,
                basketballBox.y + basketballBox.height / 2.0
        );

        for (Rect2d playerBox : playerBoxes) {
            Point playerCenter = new Point(
                    playerBox.x + playerBox.width / 2.0,
                    playerBox.y + playerBox.height / 2.0
            );

            double distance = calculateDistance(ballCenter, playerCenter);

            if (distance < minDistance) {
                minDistance = distance;

                Mat region = image.submat(
                        (int) playerBox.y, (int) (playerBox.y + playerBox.height),
                        (int) playerBox.x, (int) (playerBox.x + playerBox.width)
                );
                possessingTeam = detectShirtColor(region);
                region.release();
            }
        }
        System.out.println("Possessisdng team: " + possessingTeam);
        return possessingTeam;
    }

    private double calculateDistance(Point p1, Point p2) {
        return Math.sqrt(Math.pow(p1.x - p2.x, 2) + Math.pow(p1.y - p2.y, 2));
    }

    private void saveAnnotatedImage(String imagePath, Mat image) {
        String outputFilePath = outputDir.replace("file://", "") + "/" + imagePath + "_annotated.png";
        Imgcodecs.imwrite(outputFilePath, image);
        System.out.println("Annotated image saved: " + outputFilePath);
    }
}
