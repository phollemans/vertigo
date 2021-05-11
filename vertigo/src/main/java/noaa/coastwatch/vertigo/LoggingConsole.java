/*
 * Vertigo Project
 * Copyright (c) 2020 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.vertigo;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import java.util.logging.StreamHandler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javafx.application.Platform;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextArea;
import javafx.scene.control.ButtonType;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;

/**
 * The <code>LoggingConsole</code> class maintains an instance of a dialog
 * that shows the logging system and standard error stream messages.
 *
 * @author Peter Hollemans
 * @since 0.7
 */
public class LoggingConsole extends Dialog {

  private static final Logger LOGGER = Logger.getLogger (LoggingConsole.class.getName());

  private static LoggingConsole instance;
  private TextArea area;
  private static StringBuffer logBuffer;
  private static LogOutputStream logStream;

  /////////////////////////////////////////////////////////////////

  static {

    logBuffer = new StringBuffer();
    logStream = new LogOutputStream();

    // Redirect System.err only when not in development mode
    var mode = System.getProperty ("vertigo.devel.mode", "false");
    if (mode.equals ("false")) System.setErr (new PrintStream (logStream, true));

  } // static

  /////////////////////////////////////////////////////////////////

  /** Updates the text area of the console from the log buffer. */
  private void updateTextArea () {
    Platform.runLater (() -> {
      area.setText (logBuffer.toString());
      area.setScrollTop (Double.MAX_VALUE);
      area.appendText ("");
    });
  } // updateTextArea

  /////////////////////////////////////////////////////////////////

  /** Appends bytes written to an output stream to the log buffer. */
  private static class LogOutputStream extends OutputStream {

    @Override
    public void write (int b) throws IOException {
      logBuffer.append (new String (new byte[] {(byte) (b & 0xff)}));
      if (instance != null) instance.updateTextArea();
    } // write

    @Override
    public void write (byte b[]) throws IOException {
      logBuffer.append (new String (b));
      if (instance != null) instance.updateTextArea();
    } // write

    @Override
    public void write (byte b[], int off, int len) throws IOException {
      logBuffer.append (new String (b, off, len));
      if (instance != null) instance.updateTextArea();
    } // write
  
  } // LogOutputStream class

  /////////////////////////////////////////////////////////////////

  protected LoggingConsole () { }
  
  /////////////////////////////////////////////////////////////////

  /** Acts as a log handler that appends log messages to the log buffer. */
  public static class Handler extends StreamHandler {
    
    public Handler () {
      setOutputStream​ (logStream);
    } // Handler

    @Override
    public void publish​ (LogRecord record) {
      super.publish (record);
      flush();
      try { logStream.flush(); }
      catch (IOException e) { }
    } // publish

  } // Handler class

  /////////////////////////////////////////////////////////////////
  
  public static LoggingConsole getInstance() {

    if (instance == null) {

      var console = new LoggingConsole();
      console.setTitle ("Log Console");
      console.setResizable (true);
            
      var area = new TextArea();
      area.setWrapText (true);
      area.setEditable (false);
      area.setFont (Font.font ("Courier New", FontWeight.BOLD, Font.getDefault().getSize()));
      area.setPrefColumnCount (100);
      area.setPrefRowCount (20);
      console.area = area;

      console.getDialogPane().setContent (area);
      console.getDialogPane().getButtonTypes().add (ButtonType.CLOSE);
      console.initModality (Modality.NONE);
      console.updateTextArea();

      instance = console;
    } // if
    
    return (instance);
  
  } // getInstance

  /////////////////////////////////////////////////////////////////

} // LoggingConsole class
