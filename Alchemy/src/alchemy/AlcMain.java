/*
 *  This file is part of the Alchemy project - http://al.chemy.org
 * 
 *  Copyright (c) 2007 Karl D.D. Willis
 * 
 *  Alchemy is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  Alchemy is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with Alchemy.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package alchemy;

import alchemy.ui.*;
import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import java.lang.reflect.Method;
import java.util.ResourceBundle;

/**
 * Main class for Alchemy<br />
 * Handles all and everything - the meta 'root' reference
 */
public class AlcMain extends JFrame implements AlcConstants, ComponentListener, KeyListener {

    /** Current PLATFORM in use, one of WINDOWS, MACOSX, LINUX or OTHER. */
    public static int PLATFORM;
    /** Modifier Key to show for tool tips - This looks like '\u2318' for Apple or 'Ctrl' otherwise */
    public static String MODIFIER_KEY;

    static {
        if (PLATFORM_NAME.indexOf("Mac") != -1) {
            PLATFORM = MACOSX;
            // Mac command key symbol
            MODIFIER_KEY = "\u2318";

        } else if (PLATFORM_NAME.indexOf("Windows") != -1) {
            PLATFORM = WINDOWS;
            MODIFIER_KEY = "Ctrl";

        } else if (PLATFORM_NAME.equals("Linux")) {  // true for the ibm vm
            PLATFORM = LINUX;
            MODIFIER_KEY = "Ctrl";

        } else {
            PLATFORM = OTHER;
            MODIFIER_KEY = "Modifier";
        }
    }
    //////////////////////////////////////////////////////////////
    // ALCHEMY CLASSES AND MODULES
    //////////////////////////////////////////////////////////////
    /** Class to take care of plugin loading */
    private AlcPlugin plugins;
    /** Class of utility math functions */
    public AlcMath math = new AlcMath();
    /** Canvas to draw on to */
    public AlcCanvas canvas;
    /** User Interface Tool Bar */
    public AlcToolBar toolBar;
    /** Preferences class */
    public AlcPreferences prefs;
    /** Session class - controls automatic saving of the canvas */
    public AlcSession session;
    /** Array of the installed 'create' modules */
    public AlcModule[] creates;
    /** Array of the installed 'affect' modules */
    public AlcModule[] affects;
    /** The menu bar */
    public AlcMenuBar menuBar;
    /** Layered pane in which the canvas and toolbar sit */
    private JLayeredPane layeredPane;
    /** Resource Bundle containing language specific text */
    public final ResourceBundle bundle;
    /** Palette for the toolbar when detached */
    public AlcPalette palette;
    /** Second monitor black out window */
    private JWindow[] screens;
    //
    //////////////////////////////////////////////////////////////
    // ALCHEMY STATUS
    //////////////////////////////////////////////////////////////
    /** The currently selected create module - set to -1 initially when nothing is selected */
    public int currentCreate = -1;
    /** The currently selected affect modules */
    public boolean[] currentAffects;
    /** The number of affect modules currently selected */
    private int numberOfCurrentAffects = 0;
    /** Preferred size of the window */
    private Dimension windowSize = null;
    //
    //////////////////////////////////////////////////////////////
    // FULLSCREEN
    //////////////////////////////////////////////////////////////
    /** Toggle between windowed and fullscreen */
    private boolean fullscreen = false;
    /** For storing the old display size before entering fullscreen */
    private Dimension oldWindowSize = null;
    /** For storing the old display location before entering fullscreen */
    private Point oldLocation = null;

    /** Toggle the state of the osx menu bar on a mac */
    //private boolean macMenuBarVisible = true;
    public AlcMain() {

        super("OSXAdapter");

        // LOAD RESOURCE BUNDLE
        bundle = ResourceBundle.getBundle("alchemy/AlcResourceBundle", LOCALE);

        // LOAD PREFERENCES
        prefs = new AlcPreferences(this);

        // LOAD PLUGINS
        plugins = new AlcPlugin(this);
        System.out.println("Number of Plugins: " + getNumberOfPlugins());

        // Initialise the on/off array for current affects
        currentAffects = new boolean[plugins.getNumberOfAffectModules()];

        // Add each type of plugin
        if (plugins.getNumberOfPlugins() > 0) {
            String[] createsOrder = {"Shapes", "Inverse Shapes", "Type Shapes", "Mic Shapes"};
            String[] affectsOrder = {"Mirror", "Blindness", "Random"};
            // Extension Point Name, Array Size, Module Type
            creates = plugins.addPlugins("Create", getNumberOfCreateModules(), CREATE, createsOrder);
            affects = plugins.addPlugins("Affect", getNumberOfAffectModules(), AFFECT, affectsOrder);
        }

        // LOAD INTERFACE AND CANVAS
        loadInterface();

        // INITIALISE THE MODULES
        initialiseModules();


        // Exit Function
        addWindowListener(new WindowAdapter() {

            public void windowClosing(WindowEvent e) {
                exitAlchemy();
            }
        });

        // Set up our application to respond to the Mac OS X application menu
        registerForMacOSXEvents();

    }

    /** Load the Alchemy interface */
    private void loadInterface() {

        // Find out how big the parent screen is
        GraphicsConfiguration grapConfig = this.getGraphicsConfiguration();
        Dimension currentWindowSize = grapConfig.getBounds().getSize();

        boolean windowSet = false;
        // If there is a saved window size then us it
        if (prefs.getCanvasSize() != null) {
            Dimension savedWindowSize = prefs.getCanvasSize();
            // Make sure the window is not too big
            if (savedWindowSize.width <= currentWindowSize.height && savedWindowSize.height <= currentWindowSize.height) {
                windowSize = savedWindowSize;
                windowSet = true;
            }
        }
        
        if (!windowSet) {
            if (currentWindowSize.width < 1000) {
                windowSize = new Dimension(800, 500);
            } else {
                windowSize = new Dimension(1024, 640);
            }
        }

        // The canvas to draw on
        canvas = new AlcCanvas(this);
        // LOAD SESSION
        session = new AlcSession(this);
        // Load the palette
        palette = new AlcPalette(this);
        // User Interface toolbar
        toolBar = new AlcToolBar(this);
        // Menu Bar
        menuBar = new AlcMenuBar(this);

        if (PLATFORM == MACOSX) {
            // For some reason the menubar still displays on OSX
            // Set it to invisible
            menuBar.setVisible(false);
            // Add normally if on MacOSX as the menu is listed above
            this.setJMenuBar(menuBar);

        } else {
            // Otherwise add it to the toolbar area

            //Palette
            if (prefs.getPaletteAttached()) {
                this.setJMenuBar(menuBar);
            //Toolbar
            } else {
                toolBar.add("North", menuBar);
                toolBar.calculateTotalHeight();
            }
        }

        // LAYERED PANE
        layeredPane = new JLayeredPane();
        // Add the UI on top of the canvas
        layeredPane.add(canvas, new Integer(1));
        // LOAD SESSION
        session = new AlcSession(this);
        layeredPane.add(toolBar, new Integer(2));

        // FRAME
        layeredPane.setPreferredSize(windowSize);          // Set the window size
        this.setContentPane(layeredPane);           // Set the layered pane as the main content pane
        this.addComponentListener(this);            // Add a component listener to detect window resizing

        Image titleBarIcon = AlcUtil.createImageIcon("data/titlebar.png").getImage();
        if (titleBarIcon != null) {
            this.setIconImage(titleBarIcon);
        }

        //this.addWindowStateListener(this);          // Add a window state listener to detect window maximising
        this.addKeyListener(this);                  // Key Listener
        this.setFocusable(true);                    // Make the key listener focusable so we can get key events
        this.setTitle("Alchemy");                // Title of the frame - Dock name should also be set -Xdock:name="Alchemy"
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.pack();                                // Finalize window layout
        // Load the old location if available
        // First check it is not off screen
        if (prefs.getCanvasLocation() != null) {
            boolean onscreen = false;
            GraphicsDevice[] devices = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
            for (int i = 0; i < devices.length; i++) {
                Rectangle screenBounds = devices[i].getDefaultConfiguration().getBounds();
                if (screenBounds.contains(prefs.getCanvasLocation())) {
                    onscreen = true;
                //System.out.println("CONTAINED within: " + screenBounds);
                }
            }
            if (onscreen) {
                this.setLocation(prefs.getCanvasLocation());
            } else {
                this.setLocationRelativeTo(null);
            }
        } else {
            this.setLocationRelativeTo(null);           // Center window on screen.
        }
        // Load the palette after the main window
        if (prefs.getPaletteAttached()) {
            setPalette(true);
        } else {
            this.requestFocus();
        }

    }

    /** Test if it is ok to exit Alchemy
     * 
     * @return Passed to the Mac quit handler - do or don't quit
     */
    public boolean exitAlchemy() {
        // Ask to quit
        if (canvas.shapes.size() > 0) {
            // Text for the dialog depends on the platform
            String exitTitle, exitMessage;
            if (PLATFORM == MACOSX) {
                exitTitle = bundle.getString("quitDialogTitle");
                exitMessage = bundle.getString("quitDialogMessage");
            } else {
                exitTitle = bundle.getString("exitDialogTitle");
                exitMessage = bundle.getString("exitDialogMessage");
            }

            int result = JOptionPane.showConfirmDialog(
                    this, 
                    exitMessage, 
                    exitTitle, 
                    JOptionPane.YES_NO_OPTION, 
                    JOptionPane.INFORMATION_MESSAGE);
            
            if (result == JOptionPane.YES_OPTION) {
                exit();
                return true;
            }
            return false;

        } else {
            exit();
            return true;
        }
    }

    /** Exit Alchemy, saving preferences and doing general clean up */
    private void exit() {
        System.out.println("Closing");

        // Save the window location if not in full screen mode
        if (!isFullscreen()) {
            prefs.setCanvasLocation(this.getLocation());
            prefs.setCanvasSize(this.getSize());
        }
        if (prefs.getPaletteAttached()) {
            prefs.setPaletteLocation(palette.getLocation());
        }


        // Turn off recording if on
        if (session.isRecording()) {
            session.setRecording(false);
        }
        // Save changes to the preferences
        prefs.writeChanges();
        this.dispose();
        System.exit(0);
    }

    private void initialiseModules() {
        // Set the global access to root, canvas, and toolBar for each module
        for (int i = 0; i < creates.length; i++) {
            creates[i].setGlobals(this, canvas, toolBar);
        }
        for (int i = 0; i < affects.length; i++) {
            affects[i].setGlobals(this, canvas, toolBar);
        }

        // Set the default create module
        currentCreate = 0;
        creates[currentCreate].setup();
    }

    //////////////////////////////////////////////////////////////
    // ROOT GETTER METHODS
    //////////////////////////////////////////////////////////////
    /** Get the Window Size as a Dimension */
    public Dimension getWindowSize() {
        return windowSize;
    }

    /** Get the PLATFORM */
    public int getPlatform() {
        return PLATFORM;
    }

    /** Get the number of plugins */
    public int getNumberOfPlugins() {
        return plugins.getNumberOfPlugins();
    }

    /** Get the number of create modules */
    public int getNumberOfCreateModules() {
        return plugins.getNumberOfCreateModules();
    }

    /** Get the number of affect modules */
    public int getNumberOfAffectModules() {
        return plugins.getNumberOfAffectModules();
    }

    /** Return true if there are affect modules currently loaded */
    public boolean hasCurrentAffects() {
        if (numberOfCurrentAffects > 0) {
            return true;
        } else {
            return false;
        }
    }

    //////////////////////////////////////////////////////////////
    // ROOT SETTER METHODS
    //////////////////////////////////////////////////////////////
    /** Set the current create function */
    public void setCurrentCreate(int i) {
        // Deselect the old create module
        creates[currentCreate].deselect();
        currentCreate = i;

        // Call that module
        // Check to see if it has been loaded, if not load it and run setup()
        if (creates[i].getLoaded()) {
            creates[i].reselect();
        } else {
            creates[i].setLoaded(true);
            creates[i].setup();
        }

    }

    /** Add an affect to the current affect array to be processed
     *  The affect is added at its index value so it can easily be removed later
     */
    public void addAffect(int i) {
        numberOfCurrentAffects++;
        currentAffects[i] = true;

        // Call that module
        if (affects[i].getLoaded()) {
            affects[i].reselect();
        } else {
            affects[i].setLoaded(true);
            affects[i].setup();

        }

    }

    /** Remove an affect from the current affect array
     *  The affect is removed at its index value
     */
    public void removeAffect(int i) {
        numberOfCurrentAffects--;
        currentAffects[i] = false;
        affects[i].deselect();
    }

    //////////////////////////////////////////////////////////////
    // WINDOW CONTROLS
    //////////////////////////////////////////////////////////////
    private void resizeWindow(ComponentEvent e) {
        // Get and set the new size of the window
        windowSize = e.getComponent().getSize();
        // Resize the UI and Canvas
        toolBar.resizeToolBar(windowSize);
        canvas.resizeCanvas(windowSize);
    }

    /**
     * Method allows changing whether this window is displayed in fullscreen or
     * windowed mode. 
     * Based on code from http://gpsnippets.blogspot.com/2007/08/toggle-fullscreen-mode.html
     * 
     * @param fullscreen true = change to fullscreen, 
     *                   false = change to windowed
     */
    public void setFullscreen(boolean fullscreen) {

        //GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        GraphicsDevice[] devices = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
        // The current monitor where the main window is located
        GraphicsConfiguration grapConfig = this.getGraphicsConfiguration();
        Rectangle bounds = grapConfig.getBounds();

        if (this.fullscreen != fullscreen) {        //are we actually changing modes.
            this.fullscreen = fullscreen;           //change modes.

            // NORMAL WINDOW
            if (!fullscreen) {

                // Remove the other windows if present
                if (screens != null) {
                    //System.out.println("Remove Called");
                    for (int i = 0; i < screens.length; i++) {
                        screens[i].setVisible(false);
                        screens[i].dispose();
                    }
                    screens = null;
                }

                this.setVisible(false);                //hide the frame so we can change it.
                this.dispose();                          //remove the frame from being displayable.
                this.setUndecorated(false);              //put the borders back on the frame.
                this.setFocusable(true);
                //device.setFullScreenWindow(null);   //needed to unset this window as the fullscreen window.
                this.setSize(oldWindowSize);             //make sure the size of the window is correct.
                this.setLocation(oldLocation);           //reset location of the window
                //setAlwaysOnTop(false);
                this.setVisible(true);
                this.toFront();

            // FULLSCREEN
            } else {

                oldWindowSize = windowSize;          //save the old window size and location
                oldLocation = getLocation();

                try {
                    // If there are multiple monitors present
                    if (devices.length > 1) {
                        // As many screens as devices minus one for the primary monitor
                        screens = new JWindow[devices.length - 1];
                        int index = 0;
                        for (int i = 0; i < devices.length; i++) {
                            Rectangle screenBounds = devices[i].getDefaultConfiguration().getBounds();
                            //System.out.println(i + " - " + screenBounds);
                            // If not the current monitor
                            // make a new full size window for each
                            if (!screenBounds.equals(bounds)) {
                                screens[index] = new JWindow(this);
                                screens[index].setBackground(Color.BLACK);
                                screens[index].setBounds(screenBounds);
                                screens[index].setFocusable(false);
                                screens[index].setVisible(true);
                                index++;
                            }
                        }
                    }

                    // If on a mac and this is the primary monitor
                    // Make room for the mac menubar
                    if (PLATFORM == MACOSX) {
                        if (bounds.x == 0) {
                            bounds.setLocation(bounds.x, 22);
                        }
                    }

                    this.setVisible(false);                  //hide everything
                    this.dispose();                          //remove the frame from being displayable.
                    this.setUndecorated(true);               //remove borders around the frame
                    this.setBounds(bounds);
                    //setAlwaysOnTop(true);
                    //device.setFullScreenWindow(this);   //make the window fullscreen.
                    this.setVisible(true);                   //show the frame
                    if (prefs.getPaletteAttached()) {
                        palette.toFront();
                    }
                    this.toFront();

                } catch (Exception e) {
                    System.err.println(e);
                }
            }

            this.repaint();  //make sure that the screen is refreshed.

        }
    }

    /**
     * This method returns true is this frame is in fullscreen. False if in 
     * windowed mode.
     * 
     * @return true = fullscreen, false = windowed.
     */
    public boolean isFullscreen() {
        return fullscreen;
    }

    //////////////////////////////////////////////////////////////
    // PALETTE
    //////////////////////////////////////////////////////////////
    /** Set the toolbar into a floating palette or on the main window */
    public void setPalette(boolean seperate) {
        // PALETTE
        if (seperate) {
            // If this is not being called at startup
            if (!prefs.getPaletteAttached()) {
                toolBar.setToolBarVisible(false);
                toolBar.remove(toolBar.toolBars);
                toolBar.remove(menuBar);
            }

            // Make sure the palette will not be offscreen
            if (prefs.getPaletteLocation() != null) {
                boolean onscreen = false;
                GraphicsDevice[] devices = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
                for (int i = 0; i < devices.length; i++) {
                    Rectangle screenBounds = devices[i].getDefaultConfiguration().getBounds();
                    if (screenBounds.contains(prefs.getPaletteLocation())) {
                        onscreen = true;
                    //System.out.println("CONTAINED within: " + screenBounds);
                    }
                }
                if (onscreen) {
                    palette.setLocation(prefs.getPaletteLocation());
                } else {
                    palette.setLocation(100, 100);
                }
            } else {
                palette.setLocation(100, 100);
            }
            
            palette.addContent(toolBar.toolBars);
            palette.pack();
            palette.setVisible(true);
            palette.requestFocus();
            prefs.setPaletteAttached(true);

            if (PLATFORM != MACOSX) {
                this.setJMenuBar(menuBar);
                toolBar.calculateTotalHeight();
            }
            toolBar.detachButton.setVisible(false);

        // TOOLBAR
        } else {
            if (palette != null) {
                
                prefs.setPaletteLocation(palette.getLocation());
                palette.setVisible(false);
                //palette.dispose();
                //palette = null;
                if (PLATFORM != MACOSX) {
                    this.setJMenuBar(null);
                    toolBar.add("North", menuBar);
                    toolBar.calculateTotalHeight();
                }
                toolBar.add("South", toolBar.toolBars);
                toolBar.calculateTotalHeight();
                toolBar.detachButton.setVisible(true);
                toolBar.revalidate();
                toolBar.setToolBarVisible(true);
                // Request focus here to enable key mapping on windows
                toolBar.requestFocus();
                prefs.setPaletteAttached(false);
             
            }
        }
    }


    //////////////////////////////////////////////////////////////
    // MAC SPECIFIC
    // For the moment this code is all grouped together here
    //////////////////////////////////////////////////////////////
    /**
     * Generic registration with the Mac OS X application menu
     * Checks the platform, then attempts to register with the Apple EAWT
     * See OSXAdapter.java to see how this is done without directly referencing any Apple APIs
     */
    public void registerForMacOSXEvents() {
        if (PLATFORM == MACOSX) {
            try {
                // Generate and register the OSXAdapter, passing it a hash of all the methods we wish to
                // use as delegates for various com.apple.eawt.ApplicationListener methods
                //OSXAdapter.setQuitHandler(this, getClass().getDeclaredMethod("quit", (Class[]) null));
                OSXAdapter.setQuitHandler(this, getClass().getDeclaredMethod("exitAlchemy", (Class[]) null));
                OSXAdapter.setAboutHandler(this, getClass().getDeclaredMethod("about", (Class[]) null));
                OSXAdapter.setPreferencesHandler(this, getClass().getDeclaredMethod("preferences", (Class[]) null));
            //OSXAdapter.setFileHandler(this, getClass().getDeclaredMethod("loadFile", new Class[]{String.class}));
            } catch (Exception e) {
                System.err.println("Error while loading the OSXAdapter:");
                e.printStackTrace();
            }
        }
    }

    /**
     * General info dialog; fed to the OSXAdapter as the method to call when 
     * "About OSXAdapter" is selected from the application menu
     */
    public void about() {
        menuBar.showAboutBox();
    }

    /** 
     * General preferences dialog; fed to the OSXAdapter as the method to call when
     * "Preferences..." is selected from the application menu
     */
    public void preferences() {
        Point loc = AlcUtil.calculateCenter(prefs);
        prefs.setLocation(loc.x, loc.y);
        prefs.setVisible(true);
    }

    /** 
     * General quit handler; fed to the OSXAdapter as the method to call when a system quit event occurs
     * A quit event is triggered by Cmd-Q, selecting Quit from the application or Dock menu, or logging out 
     */
//    public boolean quit() {
//        int option = JOptionPane.showConfirmDialog(this, "Are you sure you want to quit?", "Quit?", JOptionPane.YES_NO_OPTION);
//        return (option == JOptionPane.YES_OPTION);
//    }
    /** 
     * General load file handler; fed to the OSXAdapter as the method to call when a file is dragged into the dock icon
     */
//    public void loadFile(String path) {
//        try {
//            currentImage = ImageIO.read(new File(path));
//            imageLabel.setIcon(new ImageIcon(currentImage));
//            imageLabel.setBackground((Color) colors[colorComboBox.getSelectedIndex()]);
//            imageLabel.setText("");
//        } catch (IOException ioe) {
//            System.out.println("Could not load image " + path);
//        }
//        repaint();
//    }
    /** 
     * Set the system properties - called before the interface is built 
     * This should eventually be removed once jar-builder is implemented
     */
    private static void setupMacSystemProperties() {
        // Mac Java 1.3
        //System.setProperty("com.apple.macos.useScreenMenuBar", "true");
        //System.setProperty("com.apple.mrj.application.growbox.intrudes", "true");
        //System.setProperty("com.apple.hwaccel", "true"); // only needed for 1.3.1 on OS X 10.2
        //System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Yes Test");

        // Mac Java 1.4
        //System.setProperty("apple.awt.showGrowBox", "true");
        System.setProperty("apple.laf.useScreenMenuBar", "true");
    //System.setProperty("apple.awt.fullscreencapturealldisplays", "true");

    }

    //////////////////////////////////////////////////////////////
    // KEY EVENTS
    //////////////////////////////////////////////////////////////
    public void keyPressed(KeyEvent event) {

        int keyCode = event.getKeyCode();

        // Turn off fullscreen mode with just the escape key if in fullscreen mode
        if (keyCode == KeyEvent.VK_ESCAPE) {
            if (isFullscreen()) {
                setFullscreen(false);
            }
        }

        passKeyEvent(event, "keyPressed");
    }

    public void keyTyped(KeyEvent event) {
        passKeyEvent(event, "keyTyped");
    }

    public void keyReleased(KeyEvent event) {
        passKeyEvent(event, "keyReleased");
    }

    private void passKeyEvent(KeyEvent event, String eventType) {
        // Reflection is used here to simplify passing events to each module
        try {
            // Pass to the current create module
            if (currentCreate >= 0) {
                Method method = creates[currentCreate].getClass().getMethod(eventType, new Class[]{KeyEvent.class});
                method.invoke(creates[currentCreate], new Object[]{event});
            }
            // Pass to all active affect modules
            for (int i = 0; i < currentAffects.length; i++) {
                if (currentAffects[i]) {
                    Method method = affects[i].getClass().getMethod(eventType, new Class[]{KeyEvent.class});
                    method.invoke(affects[i], new Object[]{event});
                }
            }
        } catch (Throwable e) {
            System.err.println("passKeyEvent: " + e + " " + eventType);
        }

    }

    /** Set the hotkey to trigger an application wide action
     * 
     * @param key       The key to trigger the action
     * @param title     A unique title for the action
     * @param action    The name of the action to call
     */
    public void setHotKey(int key, String title, Action action) {
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key, MENU_SHORTCUT), title);
        palette.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key, MENU_SHORTCUT), title);
        getRootPane().getActionMap().put(title, action);
        palette.getRootPane().getActionMap().put(title, action);
    }

    public void componentHidden(ComponentEvent e) {
    }

    public void componentMoved(ComponentEvent e) {
    }

    public void componentShown(ComponentEvent e) {
    }

    public void componentResized(ComponentEvent e) {
        resizeWindow(e);
    }
    /*
    public void windowStateChanged(WindowEvent e) {
    System.out.println("STATE CHANGED");
    //resizeWindow(e);
    }
     */

    public static void main(String[] args) {
        if (PLATFORM == MACOSX) {
            setupMacSystemProperties();
        }

        // Set system look and feel
        try {

            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            // Custom repaint class to manage transparency and redraw better
            // RepaintManager.setCurrentManager(new AlcRepaintManager());
            //RepaintManager.setCurrentManager(new CheckThreadViolationRepaintManager());

            //JFrame.setDefaultLookAndFeelDecorated(true);

        } catch (Exception e) {
            e.printStackTrace();
        }

        AlcMain window = new AlcMain();
        window.setVisible(true);
    }
}
