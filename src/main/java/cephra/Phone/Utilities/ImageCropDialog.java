package cephra.Phone.Utilities;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

/**
 * Dialog for cropping images to be used as profile pictures.
 * Provides a visual interface for users to select the crop area.
 */
public class ImageCropDialog extends JDialog {
    
    private BufferedImage originalImage;
    private BufferedImage croppedImage = null;
    private CropPanel cropPanel;
    private boolean confirmed = false;
    
    public ImageCropDialog(Window parent, BufferedImage image) {
        super(parent, "Crop Profile Picture", ModalityType.APPLICATION_MODAL);
        this.originalImage = image;
        
        initializeComponents();
        setupLayout();
        setupEventHandlers();
        
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        pack();
        setLocationRelativeTo(parent);
    }
    
    private void initializeComponents() {
        cropPanel = new CropPanel(originalImage);
    }
    
    private void setupLayout() {
        setLayout(new BorderLayout());
        
        // Main crop panel
        add(cropPanel, BorderLayout.CENTER);
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout());
        
        JButton cropButton = new JButton("Crop");
        cropButton.setPreferredSize(new Dimension(100, 30));
        cropButton.addActionListener(e -> performCrop());
        
        JButton cancelButton = new JButton("Cancel");
        cancelButton.setPreferredSize(new Dimension(100, 30));
        cancelButton.addActionListener(e -> dispose());
        
        buttonPanel.add(cropButton);
        buttonPanel.add(cancelButton);
        add(buttonPanel, BorderLayout.SOUTH);
        
        // Instructions panel
        JPanel instructionPanel = new JPanel();
        instructionPanel.add(new JLabel("Drag to select the area for your profile picture"));
        add(instructionPanel, BorderLayout.NORTH);
    }
    
    private void setupEventHandlers() {
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                dispose();
            }
        });
    }
    
    private void performCrop() {
        Rectangle cropArea = cropPanel.getCropArea();
        if (cropArea != null && cropArea.width > 0 && cropArea.height > 0) {
            try {
                // Calculate scaling factor
                double scaleX = (double) originalImage.getWidth() / cropPanel.getImageDisplayWidth();
                double scaleY = (double) originalImage.getHeight() / cropPanel.getImageDisplayHeight();
                
                // Adjust crop area to original image coordinates
                int x = (int) (cropArea.x * scaleX);
                int y = (int) (cropArea.y * scaleY);
                int width = (int) (cropArea.width * scaleX);
                int height = (int) (cropArea.height * scaleY);
                
                // Ensure coordinates are within bounds
                x = Math.max(0, Math.min(x, originalImage.getWidth() - 1));
                y = Math.max(0, Math.min(y, originalImage.getHeight() - 1));
                width = Math.min(width, originalImage.getWidth() - x);
                height = Math.min(height, originalImage.getHeight() - y);
                
                if (width > 0 && height > 0) {
                    croppedImage = originalImage.getSubimage(x, y, width, height);
                    
                    // Create a new BufferedImage to ensure it's not a subimage reference
                    BufferedImage finalImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                    Graphics2D g2d = finalImage.createGraphics();
                    g2d.drawImage(croppedImage, 0, 0, null);
                    g2d.dispose();
                    
                    croppedImage = finalImage;
                    confirmed = true;
                    dispose();
                } else {
                    JOptionPane.showMessageDialog(this, 
                        "Invalid crop area. Please select a valid area to crop.", 
                        "Invalid Selection", JOptionPane.WARNING_MESSAGE);
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, 
                    "Error cropping image: " + e.getMessage(), 
                    "Crop Error", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        } else {
            JOptionPane.showMessageDialog(this, 
                "Please select an area to crop by dragging on the image.", 
                "No Selection", JOptionPane.WARNING_MESSAGE);
        }
    }
    
    public BufferedImage getCroppedImage() {
        return confirmed ? croppedImage : null;
    }
    
    /**
     * Panel that displays the image and allows user to select crop area
     */
    private static class CropPanel extends JPanel {
        private BufferedImage image;
        private Rectangle cropArea = null;
        private Point dragStart = null;
        private boolean dragging = false;
        private int imageDisplayWidth;
        private int imageDisplayHeight;
        private int imageX, imageY; // Position of image within panel
        
        public CropPanel(BufferedImage image) {
            this.image = image;
            calculateDisplaySize();
            setPreferredSize(new Dimension(imageDisplayWidth + 40, imageDisplayHeight + 40));
            setBackground(Color.LIGHT_GRAY);
            
            setupMouseHandlers();
        }
        
        private void calculateDisplaySize() {
            // Max display size
            int maxWidth = 600;
            int maxHeight = 500;
            
            int originalWidth = image.getWidth();
            int originalHeight = image.getHeight();
            
            double scale = Math.min((double) maxWidth / originalWidth, 
                                  (double) maxHeight / originalHeight);
            
            imageDisplayWidth = (int) (originalWidth * scale);
            imageDisplayHeight = (int) (originalHeight * scale);
        }
        
        private void setupMouseHandlers() {
            MouseAdapter mouseHandler = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (isPointOnImage(e.getPoint())) {
                        dragStart = new Point(e.getX() - imageX, e.getY() - imageY);
                        dragging = true;
                        cropArea = new Rectangle(dragStart.x, dragStart.y, 0, 0);
                        repaint();
                    }
                }
                
                @Override
                public void mouseDragged(MouseEvent e) {
                    if (dragging && dragStart != null) {
                        Point currentPoint = new Point(e.getX() - imageX, e.getY() - imageY);
                        
                        // Constrain to image bounds
                        currentPoint.x = Math.max(0, Math.min(currentPoint.x, imageDisplayWidth));
                        currentPoint.y = Math.max(0, Math.min(currentPoint.y, imageDisplayHeight));
                        
                        int x = Math.min(dragStart.x, currentPoint.x);
                        int y = Math.min(dragStart.y, currentPoint.y);
                        int width = Math.abs(currentPoint.x - dragStart.x);
                        int height = Math.abs(currentPoint.y - dragStart.y);
                        
                        cropArea = new Rectangle(x, y, width, height);
                        repaint();
                    }
                }
                
                @Override
                public void mouseReleased(MouseEvent e) {
                    dragging = false;
                }
            };
            
            addMouseListener(mouseHandler);
            addMouseMotionListener(mouseHandler);
        }
        
        private boolean isPointOnImage(Point p) {
            return p.x >= imageX && p.x <= imageX + imageDisplayWidth &&
                   p.y >= imageY && p.y <= imageY + imageDisplayHeight;
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, 
                               RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            
            // Center the image in the panel
            imageX = (getWidth() - imageDisplayWidth) / 2;
            imageY = (getHeight() - imageDisplayHeight) / 2;
            
            // Draw the image
            g2d.drawImage(image, imageX, imageY, imageDisplayWidth, imageDisplayHeight, null);
            
            // Draw crop selection
            if (cropArea != null) {
                g2d.setColor(new Color(0, 0, 255, 100)); // Semi-transparent blue
                g2d.fillRect(imageX + cropArea.x, imageY + cropArea.y, 
                           cropArea.width, cropArea.height);
                
                g2d.setColor(Color.BLUE);
                g2d.setStroke(new BasicStroke(2));
                g2d.drawRect(imageX + cropArea.x, imageY + cropArea.y, 
                           cropArea.width, cropArea.height);
                
                // Draw corner handles
                int handleSize = 8;
                g2d.setColor(Color.WHITE);
                g2d.fillRect(imageX + cropArea.x - handleSize/2, 
                           imageY + cropArea.y - handleSize/2, handleSize, handleSize);
                g2d.fillRect(imageX + cropArea.x + cropArea.width - handleSize/2, 
                           imageY + cropArea.y - handleSize/2, handleSize, handleSize);
                g2d.fillRect(imageX + cropArea.x - handleSize/2, 
                           imageY + cropArea.y + cropArea.height - handleSize/2, handleSize, handleSize);
                g2d.fillRect(imageX + cropArea.x + cropArea.width - handleSize/2, 
                           imageY + cropArea.y + cropArea.height - handleSize/2, handleSize, handleSize);
                
                g2d.setColor(Color.BLUE);
                g2d.drawRect(imageX + cropArea.x - handleSize/2, 
                           imageY + cropArea.y - handleSize/2, handleSize, handleSize);
                g2d.drawRect(imageX + cropArea.x + cropArea.width - handleSize/2, 
                           imageY + cropArea.y - handleSize/2, handleSize, handleSize);
                g2d.drawRect(imageX + cropArea.x - handleSize/2, 
                           imageY + cropArea.y + cropArea.height - handleSize/2, handleSize, handleSize);
                g2d.drawRect(imageX + cropArea.x + cropArea.width - handleSize/2, 
                           imageY + cropArea.y + cropArea.height - handleSize/2, handleSize, handleSize);
            }
            
            g2d.dispose();
        }
        
        public Rectangle getCropArea() {
            return cropArea;
        }
        
        public int getImageDisplayWidth() {
            return imageDisplayWidth;
        }
        
        public int getImageDisplayHeight() {
            return imageDisplayHeight;
        }
    }
}
