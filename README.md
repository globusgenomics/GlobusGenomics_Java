# Java program for the automation of creating Dockerfile, Building it and pushing it to the Dockerhub.


# Within the Java program (MyDomParser.java)

Edit the following paths accordingly:

File dir = new File("/opt/galaxy/tools/");  /* Path to Globus Genomics tools */
String localDockerContext = "/scratch/go/pooja043/github/GlobusGenomics_Tools/";  /* Path to store generated Dockerfiles */
String mntLocation = "/mnt/galaxyTools/tools/";  /* Path to Binaries of Globus Genomics tools */


#Compile:

sudo javac -cp commons-exec-1.3.jar:commons-exec-1.3-sources.jar:commons-exec-1.3-test-sources.jar:commons-io-2.5-javadoc.jar:commons-exec-1.3-javadoc.jar:commons-exec-1.3-tests.jar:commons-io-2.5.jar: MyDomParser.java

#Run:

sudo java -cp commons-exec-1.3.jar:commons-exec-1.3-sources.jar:commons-exec-1.3-test-sources.jar:commons-io-2.5-javadoc.jar:commons-exec-1.3-javadoc.jar:commons-exec-1.3-tests.jar:commons-io-2.5.jar: MyDomParser