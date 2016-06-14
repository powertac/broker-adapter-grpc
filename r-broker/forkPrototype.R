runBroker <- function(input, output) {
  # wd is where the fifos and the pom.xml sit
  #setwd("..")
  pr <- parallel:::mcfork()
  if (inherits(pr, "masterProcess")) {
    # this is the child process
    args <- c("exec:exec", " -Dexec.args='", "--prop samplebroker.r.charStreamAdapter.inputFilename:", output,  " --prop samplebroker.r.charStreamAdapter.outputFilename:", input, "'")
    system2("mvn", args="exec:exec -Dexec.args='--prop samplebroker.r.charStreamAdapter.inputFilename:broker-input --prop samplebroker.r.charStreamAdapter.outputFilename:broker-output'")
    parallel:::mcexit("child exit")
  }
  # parent process
  print("start parent")
  # order of opening these fifos must match order in CharStreamAdapter.java
  outfile <- fifo(output, open="w", blocking=TRUE)
  infile <- fifo(input, open="r", blocking=TRUE)
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
