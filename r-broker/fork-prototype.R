runBroker <- function(input, output) {
  setwd("..")
  infile <- file(input, "r")
  outfile <- file(output, "w")
  pr <- parallel:::mcfork()
  if (inherits(pr, "masterProcess")) {
    # this is the child process
    system2("mvn", args="exec:exec -Dexec.args='--prop samplebroker.r.charStreamAdapter.inputFilename:broker-input --prop samplebroker.r.charStreamAdapter.outputFilename:broker-output'")
    parallel:::mcexit("child exit")
  }
  # parent process
  done = FALSE
  while (!done) {
    line <- readlines(infile, n=1)
    # process lines, send messages
    print(line)
    if (!is.na(charmatch("<ts-done", c(line)))) {
      # process input here
      writeLines(c("continue"), outfile)
    }
  }
}

runBroker("broker-output", "broker-input")