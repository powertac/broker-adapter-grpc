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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.powertac.samplebroker.core.MessageDispatcher;
import org.powertac.samplebroker.interfaces.BrokerContext;
import org.powertac.samplebroker.interfaces.Initializable;
import org.powertac.samplebroker.interfaces.IpcAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author John Collins
 */
@Service // forces Spring to create a singleton instance at startup
public class CharStreamAdapter implements IpcAdapter, Initializable
{
  static private Logger log = LogManager.getLogger(CharStreamAdapter.class);

  @Autowired
  private MessageDispatcher dispatcher;

  private InputStream inputStream;
  private PrintStream outputStream;
  boolean finished = false;

  // xml tag recognizer
  private Pattern tagRe = Pattern.compile("<(\\S+)\\s");

  @Override
  public void initialize (BrokerContext broker)
  {
    inputStream = System.in;
    outputStream = System.out;
  }

  /**
   * Exports message by putting it on stdout
   * @see org.powertac.samplebroker.interfaces.IpcAdapter#exportMessage(java.lang.String)
   */
  @Override
  public void exportMessage (String message)
  {
    outputStream.println(message);
    outputStream.flush();
  }

  /* (non-Javadoc)
   * @see org.powertac.samplebroker.interfaces.IpcAdapter#startMessageImport()
   */
  @Override
  public void startMessageImport ()
  {
    Importer imp = new Importer();
    imp.start();
    try {
      imp.join();
    }
    catch (InterruptedException ie) {
      log.warn("interrupted", ie);
    }
  }

  private synchronized void finish ()
  {
    finished = true;
  }

  synchronized boolean isFinished ()
  {
    return finished;
  }

  class Importer extends Thread
  {
    public Importer ()
    {
      super();
    }

    @Override
    public void run ()
    {
      BufferedReader input =
          new BufferedReader(new InputStreamReader(inputStream));
      StringBuffer message = new StringBuffer();
      String tag = null;
      while (!isFinished()) {
        try {
          String line = input.readLine();
          if (line.equals("quit")) {
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
                // start of otential multi-line form
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
    }
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
}
