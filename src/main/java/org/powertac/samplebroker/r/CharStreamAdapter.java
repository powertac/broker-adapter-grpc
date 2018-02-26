/*
 * Copyright (c) 2016 by John Collins
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.powertac.samplebroker.r;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.powertac.common.config.ConfigurableValue;
import org.powertac.common.msg.SimEnd;
import org.powertac.common.msg.TimeslotUpdate;
import org.powertac.samplebroker.core.BrokerPropertiesService;
import org.powertac.samplebroker.core.MessageDispatcher;
import org.powertac.samplebroker.interfaces.Activatable;
import org.powertac.samplebroker.interfaces.BrokerContext;
import org.powertac.samplebroker.interfaces.Initializable;
import org.powertac.samplebroker.interfaces.IpcAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author John Collins
 */
@Service("CharStreamAdapter") // forces Spring to create a singleton instance at startup
public class CharStreamAdapter
implements IpcAdapter, Initializable, Activatable
{
  static private Logger log = LogManager.getLogger(CharStreamAdapter.class);

  @Autowired
  private MessageDispatcher dispatcher;

  @Autowired
  private BrokerPropertiesService propertiesService;

  // input and output streams. If null, use stdin/stdout
  @ConfigurableValue(valueType = "String",
      description = "name of input file")
  private String inputFilename = "";

  @ConfigurableValue(valueType = "String",
      description = "name of output file")
  private String outputFilename = "";

  private InputStream inputStream;
  private PrintStream outputStream;
  private BrokerContext context;
  private Exporter exporter;
  private BlockingQueue<String> exportQueue;
  private Importer importer;
  private boolean finished = false;
  private boolean inputActive = false;

  @Override
  public void initialize (BrokerContext broker)
  {
    context = broker;
    propertiesService.configureMe(this);
    if (inputFilename.length() == 0) {
      if (null == inputStream)
        inputStream = System.in;
    }
    else {
      // open input file
      try {
        log.info("Reading input from {}", inputFilename);
        inputStream = new FileInputStream(inputFilename);
      }
      catch (FileNotFoundException e) {
        log.error("Cannot open input stream {}", inputFilename);
        log.error(e.toString());
      }
    }
    if (outputFilename.length() == 0) {
      if (null == outputStream)
        outputStream = System.out;
    }
    else {
      // open output file
      try {
        log.info("Writing output to {}", outputFilename);
        outputStream =
            new PrintStream(new FileOutputStream(outputFilename), true);
      }
      catch (FileNotFoundException e) {
        log.error("Cannot open output stream {}", outputFilename);
        log.error(e.toString());
      }
    }
    // set up exporter
    exportQueue = new ArrayBlockingQueue<String>(1024);
    exporter = new Exporter();
    exporter.start();
  }

  // Start the message importer on first activation
  @Override
  public void activate (int timeslot)
  {
    log.info("activate");
    if (!inputActive) {
      inputActive = true;
      startMessageImport();
    }
  }

  /**
   * Exports message by putting it on the export queue 
   * @see org.powertac.samplebroker.interfaces.IpcAdapter#exportMessage(java.lang.String)
   */
  @Override
  public void exportMessage (String message)
  {
    log.info("exporting message {}", message);
    try {
      exportQueue.put(message);
    }
    catch (InterruptedException e) {
      log.warn("interrupted put on export queue");
    }
  }

  /* (non-Javadoc)
   * @see org.powertac.samplebroker.interfaces.IpcAdapter#startMessageImport()
   */
  @Override
  public void startMessageImport ()
  {
    importer = new Importer();
    importer.start();
  }

  private synchronized void finish ()
  {
    finished = true;
    outputStream.close();
  }

  synchronized boolean isFinished ()
  {
    return finished;
  }

  class Exporter extends Thread
  {
    public Exporter ()
    {
      super();
    }

    @Override
    public void run ()
    {
      while (!finished) {
        String msg;
        try {
          msg = exportQueue.take();
          outputStream.println(msg);
          outputStream.flush();
        }
        catch (InterruptedException e) {
          log.warn("interrupted take on export queue");
        }
      }
    }
  }

  class Importer extends Thread
  {
    // xml tag recognizer
    private Pattern tagRe = Pattern.compile("<([-_\\w]+)[\\s/>]");

    public Importer ()
    {
      super();
    }

    @Override
    public void run ()
    {
      log.info("importer started");
      BufferedReader input =
          new BufferedReader(new InputStreamReader(inputStream));
      StringBuffer message = new StringBuffer();
      String tag = null;
      while (!isFinished()) {
        try {
          log.info("waiting for input");
          String line = input.readLine();
          if (null == line) {
            finish();
            continue;
          }
          log.info("input:{}", line);
          if (line.equals("abort")) {
            log.info("force exit");
            System.exit(0);
          }
          else if (line.equals("continue")) {
            log.info("release");
            //context.sendMessage(new PauseRelease(context.getBroker()));
          }
          else if (line.equals("quit")) {
            finish();
          }
          else {
            if (null == tag) {
              // start of message
              if (line.endsWith("/>")) {
                // one-line form
                dispatcher.sendRawMessage(line);
              }
              else {
                // start of potential multi-line form
                Matcher m = tagRe.matcher(line);
                if (m.lookingAt()) {
                  // capture the end-of-form string
                  tag = "</" + m.group(1) + ">";
                  if (line.endsWith(tag)) {
                    // one-line nested form
                    dispatcher.sendRawMessage(line);
                    tag = null;
                  }
                  else {
                    // definitely a multi-line form
                    message.append(line).append("\n");
                  }
                }
                else {
                  // bad start of xml form
                  log.error("bad start of xml form {}", line);
                }
              }
            }
            else {
              // subsequent lines of multi-line form
              message.append(line);
              if (line.endsWith(tag)) {
                dispatcher.sendRawMessage(message.toString());
                message = new StringBuffer();
                tag = null;
              }
              else {
                // fix EOL
                message.append("\n");
              }
            }
          }
        }
        catch (IOException e) {
          log.error(e.getMessage());
        }
      }
      try {
        log.info("importer finished");
        input.close();
      }
      catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * Handles a TimeslotUpdate message by issuing a PauseRequest.
   */
  public synchronized void handleMessage (TimeslotUpdate tu)
  {
    log.info("TimeslotUpdate {}", tu.getFirstEnabled() - 1);
    //context.sendMessage(new PauseRequest(context.getBroker()));
  }

  /**
   * Handles the sim-end message
   */
  public synchronized void handleMessage (SimEnd end)
  {
    log.info("SimEnd");
    finish();
  }

  // Test support
  void setInputStream (InputStream stream)
  {
    inputStream = stream;
  }

  void setOutputStream (PrintStream stream)
  {
    outputStream = stream;
  }

  void waitForImport ()
  {
    try {
      importer.join();
    }
    catch (InterruptedException e) {
      log.error("interrupt while waiting for import");
    }
  }
}
