package Network;

import OCR.OCRTest;
import OCR.Timer;
import SmithWaterman.SWinitialiser;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class EdgeServer {

    ArrayList<String> processedText = new ArrayList<>();
    File dir = new File("filesToProcess");
    Timer timer = new Timer();
    int test = 2;

    public EdgeServer(String address, int port) throws IOException, InterruptedException {

        //initial networking
        Thread ftpServer = new easyFTPServer(address, 2221);
        ftpServer.start();
        Socket socket = new Socket();
        InetSocketAddress sa = new InetSocketAddress(address, port);

        //initial connection to server
        socket.connect(sa, 10000);
        System.out.println("Edge server connected to server");

        int filesProcessed = 0;
        int imagesToProcess = 10;


        switch (test) {
            case 1:
                OCRBench(socket, 10);
                break;
            case 2:
                SWBench(socket, 1);
                break;
        }


    }

    public void OCRBench(Socket socket, int imagesToProcess) throws IOException {
        OCRTest ocr = new OCRTest("tessdata");
        int filesProcessed = 0;
        timer.start();
        while(filesProcessed != 10) {
            for(File image : dir.listFiles()){
                timer.newLap();
                this.processedText.add(ocr.readImage(image));
                image.delete();
                filesProcessed++;

                if(filesProcessed == imagesToProcess){
                    timer.stopAndPrint("OCR");
                    break;
                }
            }
        }
        individualTransmission(socket, this.processedText);
        //compactTransmission(socket, this.processedText);
    }

    public void SWBench(Socket socket, int iterations) throws IOException, InterruptedException {
        //NOTE: run time is affected most by query
        String[] inputFilesName = {"smallQuery", "database", "alphabet","scoringmatrix"};
        String[] inputFileString = new String[4];
        Timer timer = new Timer();
        File[] inputFiles = new File[4];
        ArrayList<String> SWOutput = new ArrayList<>();
        int m = 1; //todo replace with args?
        int k = 1;

        //File grabbing
        TimeUnit.SECONDS.sleep(3);
        try {
            timer.start();
            for(int i = 0; i < iterations; i++){
                timer.newLap();
                for(int j = 0; j < 4; j++){
                    inputFiles[j] = new File("filesToProcess\\" + inputFilesName[j]+i+ ".txt");
                    inputFileString[j] = "filesToProcess\\" + inputFilesName[j]+i+ ".txt";
                }//end of j loop
                SWOutput.add(new SWinitialiser().run(inputFileString[0], inputFileString[1], inputFileString[2], inputFileString[3], m, k));
            }//end of i loop
            timer.stopAndPrint("SW run");

            //cleanup
            File dir = new File("filesToProcess");
            for(File file : dir.listFiles()){
                file.delete();
            }

            //Sending results of Smith-Waterman
            timer.start();
            compactTransmission(socket, SWOutput);
            timer.stopAndPrint("SW Transmission");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void individualTransmission(Socket socket, ArrayList<String> manyOutput) throws IOException {
        DataOutputStream dataOutput = new DataOutputStream(socket.getOutputStream());
        Timer timer = new Timer();

        timer.start();
        for (String out : manyOutput){
            dataOutput.writeUTF(out);
            timer.newLap();
        }
        timer.stopTimer();
        timer.printResultsToFile("Individual Transmission Start");
    }

    public void compactTransmission(Socket socket, ArrayList<String> manyOutput) throws IOException {
        DataOutputStream dataOutput = new DataOutputStream(socket.getOutputStream());
        String outputString = manyOutput.get(0); //Semicolon seperated values of manyOutput
        Timer timer = new Timer();

        //allows for proper formatting
        manyOutput.remove(0);
        //Converts ArrayList to semicolon seperated values
        for(String output : manyOutput) {
            outputString = outputString + ";" + output;
        }

        //removes unnecessary new lines
        outputString = outputString.replace("\n", "").replace("\r", "");

        //times the transmission until it is done
        timer.start();
        dataOutput.writeUTF(outputString);
        timer.stopAndPrint("Compact Transmission Start");
    }

}//end of class
