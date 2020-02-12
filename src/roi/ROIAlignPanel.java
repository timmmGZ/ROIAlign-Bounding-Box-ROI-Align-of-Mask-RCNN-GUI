package roi;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class ROIAlignPanel extends JPanel {
	/**
	 * github:timmmGZ 2020/02/10
	 */
	private static final long serialVersionUID = 1L;
	private BufferedImage bi, subImg;
	private Convolution conv = new Convolution();
	private List<double[][]> vector, output = new ArrayList<double[][]>();
	public static int resolutionW = 320, resolutionH = 240, gridLen = 16, pixelSize = 2, outputSize = 7, sampleSize = 2,
			exampleFeatureId = 0, featureW, featureH, outPiSize = 7, e = 9;// "e" is example feature's pixelSize
	private static int scales[] = { 28, 56, 112 };
	private static double[][] ratios = { { 1, 1 }, { 1, 2 }, { 2, 1 } }, exampleScan;
	private double scaleW, scaleH;
	private JButton scan = new JButton("Random Bounding Box");
	private boolean scanning = false;

	public ROIAlignPanel() throws IOException {
		setLayout(null);
		setPreferredSize(new Dimension(1240, 700));
		setBackground(new Color(233, 200, 100));
		setVisible(true);
		JFrame jf = new JFrame("TimmmGZ¡ª¡ªROI Align");
		bi = ImageIO.read(new File(ROIAlignPanel.class.getResource("example.jpg").getFile()));
		convolutionProcess();
		exampleScan = new double[featureH][featureW];
		IntStream.range(0, vector.size()).forEach(i -> output.add(new double[outputSize][outputSize]));
		scan.setBounds(300, 650, 200, 30);
		scan.addActionListener(a -> drawRandomBoundingBox(this.getGraphics()));
		jf.add(scan);
		jf.add(this);
		jf.setVisible(true);
		jf.setResizable(false);
		jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		jf.pack();
		jf.setLocationRelativeTo(null);
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		g.drawImage(bi, 50, 50, null);
		setTexts(g);
		if (scanning) {
			drawFeatures(g);
			drawOneOfFeatures(g);
			drawOutputs(g);
			drawOutputVector(g);
			scan.updateUI();
			repaint();
		}
	}

	public void scan(Graphics g) {
		scanning = true;
		for (int x = 0; x < resolutionW; x += gridLen)
			for (int y = 0; y < resolutionH; y += gridLen)
				for (int scale : scales)
					for (double[] ratio : ratios) {
						repaint();
						g.setColor(Color.RED);
						int xL = x - (int) (scale * ratio[0] / 2), yT = y - (int) (scale * ratio[1] / 2),
								xR = x + (int) (scale * ratio[0] / 2), yB = y + (int) (scale * ratio[1] / 2),
								w = xR - xL, h = yB - yT;
						g.drawRect(50 + xL, 50 + yT, w, h);
						for (int i = 0; i < vector.size(); i++)
							drawROI(g, xL, yT, w, h, i / 8 * (featureW * pixelSize + 10) + 430,
									i % 8 * (featureH * pixelSize + 10) + 50, scaleW * pixelSize, scaleH * pixelSize,
									false);
						setOutput(xL * scaleW, yT * scaleH, w * scaleW, h * scaleH);
						drawROI(g, xL, yT, w, h, 840, 50, e * scaleW, e * scaleH, true);
						drawROI(g, xL, yT, w, h, 840, 350, e * scaleW, e * scaleH, true);
						drawBoundingBox(g, xL, yT, w, h, xR, yB);
						scan.updateUI();
					}
		scanning = false;
	}

	public void drawRandomBoundingBox(Graphics g) {
		super.paintComponent(g);
		paintComponent(g);
		g.setColor(Color.RED);
		Random r = new Random();
		int x = r.nextInt(resolutionW) / gridLen * gridLen;
		int y = r.nextInt(resolutionH) / gridLen * gridLen;
		int scale = scales[r.nextInt(scales.length)];
		double[] ratio = ratios[r.nextInt(ratios.length)];
		int xL = x - (int) (scale * ratio[0] / 2), yT = y - (int) (scale * ratio[1] / 2),
				xR = x + (int) (scale * ratio[0] / 2), yB = y + (int) (scale * ratio[1] / 2), w = xR - xL, h = yB - yT;
		g.drawRect(50 + xL, 50 + yT, w, h);
		setOutput(xL * scaleW, yT * scaleH, w * scaleW, h * scaleH);
		setExample(xL, yT, w, h);
		drawFeatures(g);
		drawOutputs(g);
		drawOutputVector(g);
		drawOneOfFeatures(g);
		for (int i = 0; i < vector.size(); i++)
			drawROI(g, xL, yT, w, h, i / 8 * (featureW * pixelSize + 10) + 430,
					i % 8 * (featureH * pixelSize + 10) + 50, scaleW * pixelSize, scaleH * pixelSize, false);
		drawROI(g, xL, yT, w, h, 840, 50, e * scaleW, e * scaleH, true);
		drawROI(g, xL, yT, w, h, 840, 350, e * scaleW, e * scaleH, true);
		drawBoundingBox(g, xL, yT, w, h, xR, yB);
	}

	private void convolutionProcess() {
		double[][] d = new double[240][320];
		Raster r = bi.getData();
		for (int y = 0; y < 240; y++)
			for (int x = 0; x < 320; x++) {
				int[] tmp = r.getPixel(x, y, (int[]) null);
				d[y][x] = (tmp[0] + tmp[1] + tmp[2]) / 765.0;
			}
		conv.setupAllLayers(d, 3);
		vector = conv.nFeaturesVector;
		featureW = vector.get(0)[0].length;
		featureH = vector.get(0).length;
		scaleW = (double) featureW / resolutionW;
		scaleH = (double) featureH / resolutionH;
	}

	public void drawBoundingBox(Graphics g, int xL, int yT, int w, int h, int xR, int yB) {
		if (xL >= 0 && yT >= 0 && xL + w <= resolutionW && yT + h <= resolutionH) {
			subImg = bi.getSubimage(xL, yT, w, h);
		} else {
			BufferedImage padding = bi.getSubimage(Math.max(xL, 0), Math.max(yT, 0),
					Math.min(w + Math.min(xL, 0), w - (xR - resolutionW) + Math.min(xL, 0)),
					Math.min(h + Math.min(yT, 0), h - (yB - resolutionH) + Math.min(yT, 0)));
			subImg = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
			subImg.getGraphics().drawImage(padding, Math.max(-xL, 0), Math.max(-yT, 0), Color.GREEN, null);
		}
		g.drawImage(subImg, 50 + (resolutionW - w) / 2, 350 + (resolutionH - h) / 2, w, h, null);
	}

	public void drawROI(Graphics g, int xL, int yT, int w, int h, int offsetX, int offsetY, double scaleW,
			double scaleH, boolean drawSamplePoints) {
		g.setColor(Color.RED);
		int xF = offsetX + (int) (xL * scaleW), yF = offsetY + (int) (yT * scaleH);
		double wF = w * scaleW, hF = h * scaleH;
		g.drawRect(xF, yF, (int) wF, (int) hF);

		double wSection = wF / outputSize, hSection = hF / outputSize;
		for (int c = 1; c < outputSize; c++) {
			g.drawLine((int) (xF + wSection * c), yF, (int) (xF + wSection * c), yF + (int) hF);
			g.drawLine(xF, (int) (yF + hSection * c), xF + (int) wF, (int) (yF + hSection * c));
		}
		if (drawSamplePoints) {
			exampleScan = new double[featureH][featureW];
			int sampleGaps = outputSize * (sampleSize + 1);
			for (int c = 1; c < sampleGaps; c++)
				for (int r = 1; r < sampleGaps; r++) {
					try {
						if (c % (sampleSize + 1) != 0 && r % (sampleSize + 1) != 0) {
							int sampleX = (int) (xL * this.scaleW + c * (w * this.scaleW / sampleGaps));
							int sampleY = (int) ((yT * this.scaleH) + r * ((h * this.scaleH) / sampleGaps));
							exampleScan[sampleY][sampleX] = vector.get(exampleFeatureId)[sampleY][sampleX];
							g.setColor(Color.BLUE);
							if (vector.get(exampleFeatureId)[sampleY][sampleX] == output.get(exampleFeatureId)[r
									/ (sampleSize + 1)][c / (sampleSize + 1)])
								g.setColor(Color.GREEN);
							g.fillRect((int) (xF + wSection / (sampleSize + 1) * c),
									(int) (yF + hSection / (sampleSize + 1) * r), 3, 3);
						}
					} catch (Exception e) {
					}
				}
		}
	}

	private void setExample(double xL, double yT, double w, double h) {
		exampleScan = new double[featureH][featureW];
		int sampleGaps = outputSize * (sampleSize + 1);
		for (int c = 1; c < sampleGaps; c++)
			for (int r = 1; r < sampleGaps; r++) {
				try {
					if (c % (sampleSize + 1) != 0 && r % (sampleSize + 1) != 0) {
						int sampleX = (int) (xL * this.scaleW + c * (w * this.scaleW / sampleGaps));
						int sampleY = (int) ((yT * this.scaleH) + r * ((h * this.scaleH) / sampleGaps));
						exampleScan[sampleY][sampleX] = vector.get(exampleFeatureId)[sampleY][sampleX];
					}
				} catch (Exception e) {
				}
			}
	}

	private void setOutput(double xLROI, double tYROI, double wROI, double hROI) {
		double wOfSectionROI = wROI / outputSize;
		double hOfSectionROI = hROI / outputSize;
		for (int i = 0; i < vector.size(); i++) {
			for (int xOutput = 0; xOutput < outputSize; xOutput++)
				for (int yOutput = 0; yOutput < outputSize; yOutput++) {
					double max = 0;
					for (int c = 1; c < sampleSize + 1; c++)
						for (int r = 1; r < sampleSize + 1; r++) {
							double sectionXL = (xLROI + xOutput * wOfSectionROI);
							double sectionYT = (tYROI + yOutput * hOfSectionROI);
							int sampleX = (int) (sectionXL + hOfSectionROI / (sampleSize + 1) * c);
							int sampleY = (int) (sectionYT + hOfSectionROI / (sampleSize + 1) * r);
							try {
								max = max > vector.get(i)[sampleY][sampleX] ? max : vector.get(i)[sampleY][sampleX];
							} catch (Exception e) {
								max = 0;
							}
						}
					output.get(i)[yOutput][xOutput] = max;
				}
		}
	}

	private void drawOneOfFeatures(Graphics g) {
		for (int y = 0; y < vector.get(exampleFeatureId).length; y++)
			for (int x = 0; x < vector.get(exampleFeatureId)[0].length; x++) {
				int grey = 255 - (int) (vector.get(exampleFeatureId)[y][x] * 255);
				g.setColor(new Color(grey, grey, grey));
				g.fillRect(840 + x * e, 50 + y * e, e, e);
			}
		for (int y = 0; y < output.get(exampleFeatureId).length; y++)
			for (int x = 0; x < output.get(exampleFeatureId)[0].length; x++) {
				int grey = 255 - (int) (output.get(exampleFeatureId)[y][x] * 255);
				g.setColor(new Color(grey, grey, grey));
				g.fillRect(1090 + x * e, 630 + y * e, e, e);
			}
		for (int y = 0; y < exampleScan.length; y++)
			for (int x = 0; x < exampleScan[0].length; x++) {
				int grey = 255 - (int) (exampleScan[y][x] * 255);
				g.setColor(new Color(grey, grey, grey));
				g.fillRect(840 + x * e, 350 + y * e, e, e);
			}
	}

	private void drawFeatures(Graphics g) {
		for (int i = 0; i < vector.size(); i++) {
			for (int y = 0; y < vector.get(i).length; y++)
				for (int x = 0; x < vector.get(i)[0].length; x++) {
					int grey = 255 - (int) (vector.get(i)[y][x] * 255);
					g.setColor(new Color(grey, grey, grey));
					g.fillRect(i / 8 * (featureW * pixelSize + 10) + 430 + x * pixelSize,
							i % 8 * (featureH * pixelSize + 10) + 50 + y * pixelSize, pixelSize, pixelSize);
				}
		}
	}

	private void drawOutputs(Graphics g) {
		for (int i = 0; i < output.size(); i++) {
			for (int y = 0; y < output.get(i).length; y++)
				for (int x = 0; x < output.get(i)[0].length; x++) {
					int grey = 255 - (int) (output.get(i)[y][x] * 255);
					g.setColor(new Color(grey, grey, grey));
					g.fillRect(i / 8 * (outputSize * outPiSize + 10) + 670 + x * outPiSize,
							i % 8 * (featureH * pixelSize + 10) + (featureH * pixelSize - outputSize * outPiSize) / 2
									+ 50 + y * outPiSize,
							outPiSize, outPiSize);
				}
		}
	}

	private void drawOutputVector(Graphics g) {
		List<Double> outputVector = outputsToVector(output);
		for (int i = 0; i < outputVector.size(); i++) {
			int grey = 255 - (int) (outputVector.get(i) * 255);
			g.setColor(new Color(grey, grey, grey));
			g.fillRect(i * 2 + 10, 625, 2, 2);
		}
	}

	private void setTexts(Graphics g) {
		g.setColor(Color.BLACK);
		g.drawString("¢ÙImage(320 * 240)", 180, 40);
		g.drawString("Sub-image in bounding box (with 0-padding)", 100, 340);
		g.drawString("¢ÚFeatures(" + featureW + " * " + featureH + ",zooming: *" + pixelSize + ")", 420, 40);
		g.drawString("¢ÛOutputs(" + outputSize + " * " + outputSize + ",zooming: *" + outPiSize + ")", 630, 40);
		g.drawString("Zooming first output to see sample points clearly(" + featureW + " * " + featureH + ",zooming: *"
				+ e + ")", 830, 40);
		g.drawString("sample Size: " + sampleSize * sampleSize + ", blue= samples, green= max sample", 835, 340);
		g.drawString("output of above ROI (" + +outputSize + " * " + outputSize + ",zooming: *" + e + "):", 850, 655);
		g.drawString("¢Üoutput vector consist of all outputs(" + outputSize * outputSize * output.size() + " * 1):", 200,
				620);
	}

	private List<Double> outputsToVector(List<double[][]> l) {
		return l.stream().map(f -> Arrays.stream(f).map(Arrays::stream)//
				.map(e -> e.boxed()).flatMap(s -> s)).flatMap(s -> s).collect(Collectors.toList());
	}

	public static void main(String[] args) throws IOException {
		ROIAlignPanel r = new ROIAlignPanel();
		r.scan(r.getGraphics());
	}
}
