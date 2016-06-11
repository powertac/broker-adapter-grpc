runBroker <- function() {
  pr <- parallel:::mcfork()
  if (inherits(pr, "masterProcess")) {
    # this is the child process
    setwd("..")
    system2("mvn", args="exec:exec")
    parallel:::mcexit("child exit")
  }
  # parent process
  v <- parallel:::readChild(pr)
  print("Parent")
  print(v)
}

runBroker()