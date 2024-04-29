import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Scanner;
import java.io.FileWriter;

import swiftbot.*;

public class DrawShape {

	static int[] lengths = new int[3];
	static String shape;
	static boolean x_pressed = false;


	public static void main(String[] args) throws InterruptedException {

		try {
			File stats = new File("swiftbot_stats.txt");
			if (stats.createNewFile()) {
				System.out.println("SwiftBot: stats database created.");
			} else {
				System.out.println("SwiftBot: stats file found. ");
			}
		} catch (IOException e) { 
			System.out.println("SwiftBot: error has occured please try again.");
			x_pressed= true;
		}


		// BOT speed = 0.028cm/ms
		SwiftBotAPI sb = new SwiftBotAPI();
		boolean QR_CODE_VALID = false;

		System.out.println("SwiftBot: Welcome to Draw the Shape!");
		System.out.println("SwiftBot: Please have a QR code ready with text in this format: S 25 or for a triangle T 20 20 20. Thank you.");

		sb.enableButton(Button.X, ()-> {
			stopProgram();
		});

		Scanner scanner = new Scanner(System.in);
		while (!x_pressed) {
			do {
				System.out.print("SwiftBot: When you are ready to scan the QR code please type 1: ");
				String codeEntered = scanner.next();
				if (codeEntered.equals("1")) {
					System.out.println("SwiftBot: Taking a picture in 3 seconds!");
					Thread.sleep(3000);
					BufferedImage qrCode = sb.getQRImage();
					String decodedMessage = sb.decodeQRImage(qrCode);
					if (decodedMessage.isEmpty()) {
						System.out.println("Error: Message was not detected. Please try again.");
						light(sb, "error");
					} else {
						if (validate_shape(sb, decodedMessage.toUpperCase())) {
							QR_CODE_VALID = true;
						} else {
							System.out.println("Error: restarting program...");
							light(sb, "error");
							Thread.sleep(3000);
						}
					}
				}
			} while (!QR_CODE_VALID);

			switch (shape) {
			case "S":
				drawSquare(sb);
				break;
			case "T":
				System.out.println("Triangle drawing!");
				drawTriangle(sb);
				break;
			default:
				System.out.println("Error: Invalid shape!");
				break;
			}
		}

		scanner.close();

		displayData();
	}

	public static void storeData(String data) {
		try {
			FileWriter store = new FileWriter("swiftbot_stats.txt");
			store.write(data);
			store.close();
			System.out.println("SwiftBot: successfully saved data to file.");
		} catch (IOException e) {
			System.out.println("SwiftBot: an error has occured.");
		}
	}

	public static void stopProgram() {
		x_pressed = true;
	}


	public static void light(SwiftBotAPI sb, String status) {
		int[] RED = {255, 0, 0};
		int[] GREEN = {0, 0, 255};
		int[] BLUE = {0, 255, 0};

		Underlight[] BUTTONS = {Underlight.FRONT_RIGHT, Underlight.FRONT_LEFT, Underlight.MIDDLE_RIGHT, Underlight.MIDDLE_LEFT, Underlight.BACK_RIGHT, Underlight.BACK_RIGHT};

		switch(status) {
		case "error":
			for (int i = 0; i<BUTTONS.length; i++) {
				sb.setUnderlight(BUTTONS[i], RED);
			}
			return;
		case "valid":
			for (int i = 0; i<BUTTONS.length; i++) {
				sb.setUnderlight(BUTTONS[i], GREEN);
			}
			return;
		case "drawing":
			for (int i = 0; i<BUTTONS.length; i++) {
				sb.setUnderlight(BUTTONS[i], BLUE);
			}
			return;
		default:
			for (int i = 0; i<BUTTONS.length; i++) {
				sb.disableUnderlights();
			}
		}
	}


	public static boolean isTPossible() {
		if (lengths.length == 3) {
			return ((lengths[0] + lengths[1] > lengths[2]) && 
					(lengths[0] + lengths[2] > lengths[1]) && 
					(lengths[1] + lengths[2] > lengths[0])); 
		} else {
			return false; 
		}
	}

	public static double calculateAngles(double a, double b, double c) {
		double angleInRadians = Math.acos((a * a + b * b - c * c) / (2 * a * b));
		return Math.toDegrees(angleInRadians);
	}


	public static float calculateTimeInMillis(int i) {
		double SPEED = 0.028f;
		if (lengths[i] != 0) {
			return (float) (lengths[i] / SPEED); 
		} else {
			return 0;
		}
	}

	public static boolean drawTriangle(SwiftBotAPI sb) {
		if (!isTPossible()) {
			System.out.println("Error: Triangle not possible");
			light(sb, "error");
			return false;
		}
		System.out.println("Triangle is possible!");

		double[] angles = new double[3];
		angles[0] = calculateAngles(lengths[0], lengths[1], lengths[2]);
		angles[1] = calculateAngles(lengths[2], lengths[0], lengths[1]);
		angles[2] = calculateAngles(lengths[1], lengths[2], lengths[0]);

		int[] angleMovements = new int[3];
		angleMovements[0] = (int) Math.round(3.5 * angles[0]);
		angleMovements[1] = (int) Math.round(3.5 * angles[1]);
		angleMovements[2] = (int) Math.round(3.5 * angles[2]);

		light(sb, "drawing");
		long startTime = System.nanoTime();
		for (int i = 0; i < 3; i++) {
			sb.move(100, -100, angleMovements[i]); 
			int time = (int) Math.ceil(calculateTimeInMillis(i)); 
			sb.move(100, 100, time); 
			sb.stopMove();
		}
		long endTime = System.nanoTime();
		long timeInSecs = (long) ((endTime - startTime)*0.0000000010);
		String roundedTime = String.format("%.1f", timeInSecs);
		System.out.println("SwiftBot: triangle successfully drawn in: " + roundedTime + " seconds.");
		String sizes = "";
		for (int i =0; i < 3; i++) {
			sizes += lengths[0];
		}
		storeData("T " + sizes + " roundedTime");

		return true;
	}


	public static void drawSquare(SwiftBotAPI sb) {
		int time = 0;
		try {
			time = (int) Math.ceil(calculateTimeInMillis(0));
		} catch (Exception e) {
			System.out.println("SwiftBot: error calculating time for side.");
		}

		light(sb, "drawing");
		long startTime = System.nanoTime();
		for (int i =0; i < 4; i++ ) {
			sb.move(100, 100, time);
			sb.stopMove();
			sb.move(100, -100, 315);
		}
		long endTime = System.nanoTime();
		long timeInSecs = (long) ((endTime - startTime)*0.0000000010);
		String roundedTime = String.format("%.1f", (double)timeInSecs);
		System.out.println("SwiftBot: square successfully drawn in: " + roundedTime + " seconds.");
		String sizes = "";
		for (int i =0; i < 1; i++) {
			sizes += lengths[0];
		}
		storeData("S " + sizes + " roundedTime");
	}

	public static void displayData() {
		try {
			File stats = new File("swiftbot_stats.txt");
			Scanner reader = new Scanner(stats);
			while (reader.hasNextLine()) {
				String data = reader.nextLine();
				System.out.println(data);
				;			}
		} catch (IOException e) {
			System.out.println("SwiftBot: an error occurred.");
		}
	}

	//	public static void drawCircle(SwiftBotAPI sb) {
	//
	//		light(sb, "drawing");
	//		sb.move(0, -100, 10000);
	//
	//	}

	public static boolean validate_shape(SwiftBotAPI sb, String decodedMessage) {
		String[] shapeInfo = decodedMessage.split(" ");

		switch (shapeInfo[0]) {
		case "S":
			if (shapeInfo.length != 2) {
				System.out.println("Error: insufficient information for square.");
				light(sb, "error");
				return false;
			}
			try {
				int length = Integer.parseInt(shapeInfo[1]);
				if (length >= 15 && length <= 85) {
					lengths[0] = length;
					shape = "S";
					return true;
				} else {
					System.out.println("Error: square length should be between 15cm and 85cm.");
					light(sb, "error");
					return false;
				}
			} catch (NumberFormatException e) {
				System.out.println("Error: square length is invalid.");
				light(sb, "error");
				return false;
			}
		case "T":
			if (shapeInfo.length != 4) {
				System.out.println("Error: insufficient information for triangle.");
				light(sb, "error");
				return false;
			}
			try {
				for (int i = 1; i <= 3; i++) {
					int length = Integer.parseInt(shapeInfo[i]);
					if (length < 15 || length > 85) {
						System.out.println("Error: Side number " + i + " is invalid.");
						light(sb, "error");
						return false;
					}
					lengths[i - 1] = length;
				}
				shape = "T";
				return true;
			} catch (NumberFormatException e) {
				System.out.println("Error: triangle lengths are invalid.");
				light(sb, "error");
				return false;
			}
		default:
			System.out.println("Error: invalid shape! Please try again.");
			light(sb, "error");
			return false;
		}
	}


}
