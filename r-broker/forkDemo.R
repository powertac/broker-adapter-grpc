# Runs broker-adapter in forked process
runBroker <- function(input, output) {
  # wd is where the fifos and the pom.xml sit
  #setwd("..")
  pr <- parallel:::mcfork()
  if (inherits(pr, "masterProcess")) {
    # this is the child process
    #print("child process")
    childOut <- fifo(input, open="w", blocking=TRUE)
    childIn <- fifo(output, open="r", blocking=TRUE)
    args <- c("exec:exec", " -Dexec.args='", "--prop samplebroker.r.charStreamAdapter.inputFilename:", output,  " --prop samplebroker.r.charStreamAdapter.outputFilename:", input, "'")
    #print(paste(args))
    #writeLines(args, childOut)
    #writeLines(paste(args, collapse=""), childOut)
    #writeLines(c("<tsx/>", "<ts-done ts=32/>"), childOut)
    system2("mvn", args=paste(args, collapse=""))
    print("child finished")
    close(childOut)
    parallel:::mcexit("child exit")
  }
  # parent process
  print("start parent")
  infile <- fifo(input, open="r", blocking=TRUE)
  outfile <- fifo(output, open="w", blocking=TRUE)
  done <- FALSE
  while (!done) {
    line <- readLines(infile, n=1)
    if (length(line) == 0) {
      print("parent done")
      done <- TRUE
    }
    # process lines, send messages
    print(line)
    if (!is.na(charmatch("<ts-done", c(line)))) {
      # process input here
      writeLines(c("continue"), outfile)
    }
  }
  close(infile)
  close(outfile)
}

# Assumes broker-adapter will be started externally after this fn is invoked
readBroker <- function(input, output) {
  infile <- fifo(input, open="r", blocking=TRUE)
  outfile <- fifo(output, open="w", blocking=TRUE)
  done <- FALSE
  while (!done) {
    line <- readLines(infile, n=1)
    if (length(line) == 0) {
      print("parent done")
      done <- TRUE
    }
    # process lines, send messages
    print(line)
    if (!is.na(charmatch("<ts-done", c(line)))) {
      # process input here
      writeLines(c("continue"), outfile)
    }
  }
  close(infile)
  close(outfile)
}

# useful cmds
start <- function() {
  setwd("development/powertac/broker-adapter")
  runBroker("broker-output", "broker-input")
}