

import java.io.File;
import java.security.AllPermission;
import java.util.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.Executor;
import org.apache.commons.io.FileUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import static java.lang.System.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import java.lang.*;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;

public class MyDomParser {
	
	public class HelperClass{
		String key;
		List<String> tools;
		
		public String getKey() {
			return key;
		}
		public void setKey(String key) {
			this.key = key;
		}
		public List<String> getTools() {
			return tools;
		}
		public void setTools(List<String> tools) {
			this.tools = tools;
		}
		
	}

	public static void main(String[] args) {

		MyDomParser parser = new MyDomParser();
		
		File dir = new File("/opt/galaxy/tools/");
		String localDockerContext = "/scratch/go/pooja043/github/GlobusGenomics_Tools/";
		String mntLocation = "/mnt/galaxyTools/tools/";
		//Get all XML Files
		ArrayList<File> listOfFiles = parser.allXmlFiles(dir);
		System.out.println(listOfFiles.size());
		
		
		
			//Parse each XML files
		for (File xmlFile : listOfFiles)
		{
			System.out.println(xmlFile.getAbsolutePath());
			
			HelperClass toolsMap = parser.parseXML(xmlFile);
			
			List<String> tools  = toolsMap.getTools();
			if (tools.size() == 0)
			{
				continue;
			}
			
			ArrayList<String> envTools = new ArrayList();
			ArrayList<String> envPaths = new ArrayList();
			
			
			
			//copy Default folder for each tool xml file
			for (String tool: tools)
			{
				File destination = parser.copyDefault(mntLocation, localDockerContext, tool, toolsMap.getKey());
				if(destination == null)
				{
					continue;
				}
				
				//Using the destination folder, parse the env file
				//List<String> envs = parser.parseEnv(new File (destination.getPath() + "/env.sh"), 
				//										tool, destination.getName(), toolsMap.getKey());
				HashMap map = parser.parseEnv(new File (destination.getPath() + "/env.sh"), tool, destination.getName(), toolsMap.getKey());				
				envTools.addAll((ArrayList<String>) map.get("envs"));
				envPaths.addAll((ArrayList<String>) map.get("tools"));
			}
			
			
			//Write Dockerfile
			File dockerfilePath = parser.writeDockerfile(envTools, envPaths, localDockerContext, toolsMap.getKey());
			System.out.println(dockerfilePath);
		//	System.exit(0);			

				
			// Build the Docker images
			//parser.buildImage(localDockerContext+toolsMap.getKey(), toolsMap.getKey().toLowerCase());
			
			//Delete default directories
			//for(String tool : tools)
			//{
			//	parser.deleteDefault(new File(localDockerContext+toolsMap.getKey()+"/"+tool));
				
			//}
			
			//Push Docker image to Docker Hub (bd2kbdds organization)
			//parser.pushImage(localDockerContext+toolsMap.getKey(), toolsMap.getKey().toLowerCase());
			
			//Remove Docker image after pushing to Docker Hub
			//parser.removeImage(localDockerContext+toolsMap.getKey(), toolsMap.getKey().toLowerCase());
			//	System.exit(0);			
			
		}

	
	}


	/* Method to get all .xml files */

	public ArrayList<File> allXmlFiles(File dir)
	{

		String xmlfile = null;
		ArrayList<File> files1 = new ArrayList<>();

		List<File> files = (List<File>) FileUtils.listFiles(
				dir, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE);
		for(File file : files)
		{
			xmlfile = file.getName();
			if(xmlfile.endsWith(".xml")||xmlfile.endsWith(".XML"))
			{
				files1.add(file);
			}

		}
		return files1;
	}



	/* Method to parse .xml files 
	 * input: path of xml files i.e out of allXmlFiles()
	 * output: list of tools*/

	public HelperClass parseXML(File file1)
	{
		String a =null;
		List<String> list1 = new ArrayList<String>();
		String id = "";

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		try
		{
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.parse(file1.toString());
			Element root = doc.getDocumentElement();
			id = root.getAttribute("id");
			
			NodeList requirementList = doc.getElementsByTagName("requirements");
			for(int k=0;k<requirementList.getLength();k++) 
			{
				Node r = requirementList.item(k);
				if(r.getNodeType() == Node.ELEMENT_NODE)
				{
					Element requirement = (Element) r;
					NodeList nameList = requirement.getChildNodes();
					for(int j=0;j<nameList.getLength();j++)
					{
						Node n = nameList.item(j);                         
						if(n.getNodeType() == Node.ELEMENT_NODE)
						{
							Element name3 = (Element) n;
							a = name3.getTextContent();
							list1.add(a);
							System.out.println(a);
						}
					}
				}
			}

		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		HelperClass toolIdMap = new HelperClass();
		toolIdMap.setKey(id);
		toolIdMap.setTools(list1);
		return toolIdMap;

	}


	/* Method to copy default -
	 * input: output of parseXML + /default 
	 * output: Target of symlink(default) i.e originalFile */

	public File copyDefault(String mntLocation, String localDockerContext, String tool, String toolId)
	{
		File file = new File(mntLocation+tool+"/default");
		File newDir = null;
		File originalFile = null;
		try 
		{

			originalFile = new File(
					Files.readSymbolicLink(Paths.get(file.getAbsolutePath())).toString());
			File dest = new File(localDockerContext + toolId + "/" + tool);
			System.out.println(dest.getAbsolutePath());

			newDir = new File(dest, originalFile.getName());
			System.out.println("CREATED DIR: "
					+ newDir.getAbsolutePath());
			newDir.mkdirs();

			//File file1 = new File(file + originalFile);
			try 
			{
				FileUtils.copyDirectory(file, newDir);
			}  catch (IOException e) {
				e.printStackTrace();
			}
		}	 catch (IOException x) {
			System.err.println(x);
		}
		return newDir;

	}


	/* Method to parse env.sh 
	 * input: (output parseXml())/output(copyDefault()) 
	 * output: list of Dockerfile lines*/

	public HashMap parseEnv(File file, String tool, String destinationName, String toolId)
	{

		HashMap<String, ArrayList<String>> map = new HashMap();
		
		//File fileName = new File(file+"/env.sh");
		String path = "/usr/local/" + toolId + "/";
		String copyTo = path + tool + "/" + destinationName;
		
		ArrayList<String> copyList = new ArrayList<String>();
		ArrayList<String> envList = new ArrayList<String>();
		
		//list.add("FROM ubuntu:16.04");
		//list.add(" ");
		
		copyList.add("COPY " + tool + "/ " + copyTo+ "/");
		
		/*list.add(" ");
		list.add("RUN " + "\\");
        list.add("  apt-get update " + "&& " + "\\");
        list.add("  apt-get install -y python");
		*/

		FileReader input;
		try 
		{
			input = new FileReader(file);
			BufferedReader bufRead = new BufferedReader(input);
			String myLine = null;

			while ( (myLine = bufRead.readLine()) != null)
			{
				if(myLine.startsWith("PATH"))
				{
					String[] array = myLine.split("=");
					envList.add(" ");
					String[] mntArray = array[1].split("/mnt/galaxyTools/tools/");
					envList.add("ENV PATH " + path + mntArray[1] +  "\"");
					
					//CharSequence cs1 = "java";
					/*boolean val = array[1].contains(cs1);
					if (val == true)
					{
						list.add("RUN apt-get update &&" + "\\" +
								"apt-get install -y default-jdk &&" + "\\" +
								"apt-get clean");
						list.add(" ");
					}*/
				} else if (myLine.startsWith("export"))
				{
					String[] array1 = myLine.split("=");
					//String[] array2 = array1[0].split(" ");
					//list.add("ENV " + array2[1] + " \"/usr/local/" + list1.get(0) + 
					//":/usr/local/" + list1.get(1) + "\"");
					envList.add("ENV PATH \"$" + copyTo + ":$PATH" + "\"");
				}

			}

		}  catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		map.put("tools", copyList);
		map.put("envs", envList);
		return map;
	}

	/* Method to write Dockerfile
	 * input: output of parseEnv and output of allXmlFiles()
	 * output: path/of/Dockerfile */

	public File writeDockerfile (ArrayList<String> envTools, ArrayList<String> envPaths, String localDockerContext, String toolId)
	{
		try
		{
			FileWriter fw = new FileWriter(localDockerContext + toolId  + "/Dockerfile");
			PrintWriter out1 = new PrintWriter(fw);
			List<String> list = new ArrayList<String>();
			
			list.add("FROM bd2kbdds/base_image");
			list.add(" ");

			list.addAll(envPaths);
	        
	        if(envTools.contains("java") == true || envTools.contains("JAVA") == true)
	        {
	        	list.add("RUN apt-get update &&" + "\\" +
						"apt-get install -y default-jdk &&" + "\\" +
						"apt-get clean");
				list.add(" ");
	        }
	       
	        list.addAll(envTools);
	        
	      for (String s : list) 
			{
				out1.println(s);
			}
	        
			out1.close();

		} catch (IOException e){
			out.println("Error!");
		}
		File dockerfilePath = new File(localDockerContext + toolId +  "/Dockerfile.txt");
		return dockerfilePath;
	}

	
	
	/* Method to build Dockerfile */
	
	public void buildImage(String buildContextPath, String toolId)
	{
		Executor exec = new DefaultExecutor();
		exec.setWorkingDirectory(new File(buildContextPath));
		CommandLine cl = CommandLine.parse("/usr/bin/docker build -t bd2kbdds/" + toolId + " .");
		int execValue;
		try {
			execValue = exec.execute(cl);
		} catch (ExecuteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/* Method to delete default 
	 * input: path/to/default*/

	public void deleteDefault(File file)
	{
		try {
			FileUtils.deleteDirectory(file);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/* Method to push Docker image*/

	public void pushImage(String buildContextPath, String toolId)
	{
		Executor exec = new DefaultExecutor();
		exec.setWorkingDirectory(new File(buildContextPath));
		CommandLine cl = CommandLine.parse("/usr/bin/docker push bd2kbdds/" + toolId);
		int execValue;
		try {
			execValue = exec.execute(cl);
		} catch (ExecuteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void removeImage(String buildContextPath, String toolId)
	{
		Executor exec = new DefaultExecutor();
		exec.setWorkingDirectory(new File(buildContextPath));
		CommandLine cl = CommandLine.parse("/usr/bin/docker rmi bd2kbdds/" + toolId);
		int execValue;
		try {
			execValue = exec.execute(cl);
		} catch (ExecuteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}


