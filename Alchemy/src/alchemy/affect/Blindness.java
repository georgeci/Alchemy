/**
 * Blindness.java
 *
 * Created on December 5, 2007, 1:52 PM
 *
 * @author  Karl D.D. Willis
 * @version 1.0
 */
package alchemy.affect;

import alchemy.*;
import alchemy.ui.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

public class Blindness extends AlcModule implements AlcConstants {

    private boolean blindShapes = false;
    private AlcSubToolBarSection subToolBarSection;

    /** Creates a new instance of Blindness */
    public Blindness() {
    }

    @Override
    public void setup() {
        canvas.setRedraw(false);

        // Create the toolbar section
        createSubToolBarSection();
        // Add the toolbar section to the main toolbar
        toolBar.addSubToolBarSection(subToolBarSection);

    }

    @Override
    public void reselect() {
        // Readd the toolbar section
        toolBar.addSubToolBarSection(subToolBarSection);
        canvas.setRedraw(false);
    }

    @Override
    public void deselect() {
        // Turn drawing back on and show what is underneath
        canvas.setRedraw(true);
        canvas.redraw();
    }

    private void redrawOnce() {
        canvas.setRedraw(true);
        canvas.redraw();
        canvas.setRedraw(false);
    }

    public void createSubToolBarSection() {
        subToolBarSection = new AlcSubToolBarSection(this);

        // Buttons
        AlcSubButton redrawButton = new AlcSubButton("Redraw", AlcUtil.getUrlPath("redraw.png", getClassLoader()));
        redrawButton.setToolTipText("Redraw the screen (b)");

        redrawButton.addActionListener(
                new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        redrawOnce();
                    }
                });
        subToolBarSection.add(redrawButton);

        AlcSubToggleButton blindShapeButton = new AlcSubToggleButton("Blind Shapes", AlcUtil.getUrlPath("redraw.png", getClassLoader()));
        blindShapeButton.setToolTipText("Redraw the screen after each shape");

        blindShapeButton.addActionListener(
                new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        toggleBlindShapes();
                    }
                });
        subToolBarSection.add(blindShapeButton);
    }

    private void toggleBlindShapes() {
        if (blindShapes) {
            blindShapes = false;
        } else {
            blindShapes = true;
        }
    }

    // MOUSE EVENTS
    @Override
    public void mouseReleased(MouseEvent e) {
        if (blindShapes) {
            redrawOnce();
        }
    }

    // KEY EVENTS
    @Override
    public void keyReleased(KeyEvent e) {
        int keyCode = e.getKeyCode();
        char keyChar = e.getKeyChar();

        //System.out.println(keyChar);
        switch (keyChar) {
            case 'b':
                redrawOnce();
                break;
        }

    /*
    switch(keyCode){
    case BACKSPACE:
    case DELETE:
    //System.out.println("DELETE");
    //canvas.clear();
    break;
    case SPACE:
    //System.out.println("SPACE");
    break;
    }
     */
    }
}