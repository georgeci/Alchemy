/**
 * AlcMainToolBar.java
 *
 * Created on November 28, 2007, 9:29 PM
 *
 * @author  Karl D.D. Willis
 * @version 1.0
 */

package alchemy.ui;

import alchemy.*;
import java.awt.*;
import javax.swing.JPanel;

public class AlcMainToolBar extends JPanel {
    
    AlcMain root;
    AlcToolBar parent;
    private int height = 60;
    
    /** Creates a new instance of AlcMainToolBar */
    public AlcMainToolBar(AlcMain root, AlcToolBar parent) {
        
        this.root = root;
        this.parent = parent;
        
        //this.setBounds(0, 0, root.getWindowSize().width, this.height);
        
        this.setLayout(new FlowLayout(FlowLayout.LEFT));
        //this.setAlignmentX(Component.LEFT_ALIGNMENT);
        //this.setAlignmentY(Component.TOP_ALIGNMENT);
        
        this.setPreferredSize(new Dimension(root.getWindowSize().width, height));
        
        
    }
    
    // Override the paint component to draw the gradient bg
    @Override protected void paintComponent( Graphics g ) {
        super.paintComponent( g );
        
        int panelWidth = getWidth();
        GradientPaint gradientPaint = new GradientPaint( 0 , 0 , parent.toolBarBgStartColour , 0 , this.height , parent.toolBarBgEndColour, true );
        if( g instanceof Graphics2D ) {
            Graphics2D g2 = (Graphics2D)g;
            // Turn on text antialias - windows does not use it by default
            //g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setPaint( gradientPaint );
            g2.fillRect( 0 , 0 ,  root.getWindowSize().width , this.height );
            g2.setPaint( parent.toolBarLineColour );
            g2.drawLine(0, this.height-1,  root.getWindowSize().width, this.height-1);
        }
    }
    
    public int getHeight(){
        return this.height;
    }
    
}