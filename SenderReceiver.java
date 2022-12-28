/* 
 *
 * Usage java SenderReceiver 0 port file       (send mode)
 *       java SenderReceiver 1 host port file  (recv mode)
 */

import java.net.Socket;
import java.net.ServerSocket;
import java.io.FileOutputStream;
import java.io.FileInputStream;

public class SenderReceiver {

  public static int BUFSIZ;

public static void main(String [] a) throws Exception {

  SenderReceiver s = new SenderReceiver();

  BUFSIZ = 1500; 
  if (a.length < 2) {
    System.out.println(
      " java SenderReceiver 0 port file       (send mode)\n" +
      " java SenderReceiver 1 host port file  (recv mode)");
    System.exit(1);
  }

  s.run(a);

} // main()

public void run(String [] a) throws Exception {

  int mode = Integer.parseInt(a[0]);              /** parseInt() is OK here */

  switch(mode) {
   case 0: dosend(a); break;
   case 1: dorecv(a); break;
  }
} /* run() */

public void dosend(String [] a) throws Exception {

  /* add all your code here */
  try {
    //Socket creation
    ServerSocket serverSocket = new ServerSocket(Integer.parseInt(a[1]));
    Socket socket = serverSocket.accept();

    byte[] fileContents = new byte[BUFSIZ];
    byte[] encodedArray = new byte[BUFSIZ];
    FileInputStream fis = new FileInputStream(a[2]);
    int packetSize = BUFSIZ;
    int[] numbers = new int[BUFSIZ/4];
    int[] bits = new int[32];
    int arrayIterator = 0, numPopulated = 0, totalBytesSent = 0, fileSize = 0, totalNumbersSent = 0, fileArrayIter = 0;

    int noOfByteSenderReceiveread = fis.read(fileContents, 0, packetSize);
    fileSize += noOfByteSenderReceiveread;

    //Loop for traversing all the bytes taken from the file
    while (noOfByteSenderReceiveread != -1){
      if(noOfByteSenderReceiveread != packetSize){
        packetSize = noOfByteSenderReceiveread;
      }

      //Logic to covert byte array to int array. Convert each char byte from file array to a digit. 
      //From the digits create a number. Add the number to the int array 
      int tempNumber = 0;
      boolean arrayUpdateFlag = false;

      //Loop executes until the maximum of numbers for a buffersize is converted or till end of file.
      while(numPopulated < numbers.length){
        if (fileContents[fileArrayIter] >= 48 && fileContents[fileArrayIter] <= 57){
          int num = fileContents[fileArrayIter] - 48;
	        if (tempNumber == 0){
	        tempNumber += num;
	        } else {
	          tempNumber = (tempNumber*10) + num;
	        }
          arrayUpdateFlag = true;
        } else if (arrayUpdateFlag){
          numbers[numPopulated++] = tempNumber;
          tempNumber = 0;
          arrayUpdateFlag = false;
        }

        //When the file array ends, read again from the file. if EOF, break the loop.
        if(fileArrayIter == (noOfByteSenderReceiveread-1)){
          noOfByteSenderReceiveread = fis.read(fileContents, 0, packetSize);
          if (noOfByteSenderReceiveread != -1){
            fileSize += noOfByteSenderReceiveread;
            fileArrayIter = -1;
          } else {
            if (arrayUpdateFlag){
              //Read the last number that was getting converted
              numbers[numPopulated++] = tempNumber;
              tempNumber = 0;
              arrayUpdateFlag = false;
            }
            break;
          }
        }
        fileArrayIter++;
      }
      packetSize = numPopulated*4;
      //Logic to convert int to bytes. 
      for(int i = 0; i < numPopulated; i++){

        //Take each number and convert it into a 32 bit binary.
        for(int j = 31; j >= 0; j--){
          bits[j] = numbers[i]%2;
          numbers[i] /= 2;
        }

        //Encoding a number into 32 bit unsigned big endian and adding the byte in the big endian method to the transmitting byte array.
        int processBits = 0;
        do{
          int byteValue = 0;
          int powerValue = 128;

	        //Converting a byte into its corresponding decimal and assigning to the byte array
	        for(int j = 0; j < 8; j++){
            if (powerValue == 0)
              powerValue = 1;
            byteValue += bits[processBits]*powerValue;
            powerValue /= 2;
            processBits++;
          }
          if (byteValue > 127)
            byteValue -= 256;
          encodedArray[arrayIterator++] = (byte)byteValue;
        } while(processBits<32);

        //If the maximum packet size is reached send the packet to client
        if (arrayIterator == packetSize){
          totalBytesSent += packetSize;
          totalNumbersSent += numPopulated;
          socket.getOutputStream().write(encodedArray, 0, packetSize);
          arrayIterator = 0;
        }
      }
      numPopulated = 0;
    }

    System.out.println("sent "+totalNumbersSent+" numbers, file "+fileSize+" bytes, transmit "+totalBytesSent+" bytes");

    fis.close();
    socket.close();
    serverSocket.close();
  
  } catch(Exception e) {
    System.out.println(e.getMessage());
  }

} /* dosend */

public void dorecv(String [] a) throws Exception {

  /* add your code here */
  try{
    Socket socket = new Socket(a[1], Integer.parseInt(a[2]));
    byte[] fileContents = new byte[BUFSIZ];
    int noOfByteSenderReceiveread = socket.getInputStream().read(fileContents, 0, BUFSIZ);

    int packetSize = BUFSIZ;
    FileOutputStream fos = new FileOutputStream(a[3]);

    int[] bits = new int[32];
    int[] numbers = new int[(BUFSIZ/4)+1];
    int[] partialBuffer = new int[3];
    byte[] fileArray = new byte[BUFSIZ];
    int[] digits = new int[10];
    int recvNumbers=0, recvFileSize=0, recvBytes=noOfByteSenderReceiveread;
    int remainingBytes = 0, bytesIterator = 0;
    boolean partialRecv = false, useBalanceBytes = false;
    

    // Executing the logic until there are no more bytes to be read.
    while(noOfByteSenderReceiveread != -1) {		
      //i is the numbers array iterator. k is the bits array iterator.	
      int arrayIterator=0, k=0, i=0, countOfBytes=0;
      if(noOfByteSenderReceiveread != BUFSIZ){
        packetSize = noOfByteSenderReceiveread;
      } else {
        packetSize = BUFSIZ;
      }

      //Server sends data as bytes. Converting the bytes to 32 bits by taking every 4 indices in the array.
      while(i <= numbers.length && countOfBytes < noOfByteSenderReceiveread){
        int temp = 0; 
        //Converting 4 bytes to bits since the number sent is encoded to 32 bits. 
        if(noOfByteSenderReceiveread >= ((i)*4+(4-remainingBytes))){
          for(int j=0; j<4; j++){
            //For handling partial bytes of data 
            if(partialRecv && remainingBytes > 0){
              for(int n = 0; n < remainingBytes; n++){
                temp = (int) partialBuffer[n];
                if(temp < 0){
                  temp = (int) partialBuffer[n] + 256;
                }
                //Converting each byte to bits.
                for(int m=((n+1)*8)-1; m >= (n*8); m--){
                  bits[m] = temp%2;
                  temp /= 2;
                }
              }
              partialRecv = false;
              useBalanceBytes = false;
            }
            if(j > 4-remainingBytes-1 && !useBalanceBytes){
              break;
            }
            temp = (int) fileContents[j+(i*4)+((useBalanceBytes) ? (-remainingBytes) : (0))];
            int bitsIter = j;
            if(!useBalanceBytes && remainingBytes>0){
              bitsIter += remainingBytes;
            }
            if(temp < 0){
              temp = (int) fileContents[j+(i*4)+((useBalanceBytes) ? (-remainingBytes) : (0))] + 256;
            }
            
            //Converting each byte to bits.
            for(k = ((bitsIter+1)*8)-1; k >= (bitsIter*8); k--){
              bits[k] = temp%2;
              temp /= 2;
            }
            countOfBytes++;
            
          }
          if(noOfByteSenderReceiveread == ((i*4)+(4-remainingBytes))){
            remainingBytes = 0;
            partialRecv = false;
            useBalanceBytes = false;
          }
          if (remainingBytes>0) {
            useBalanceBytes = true;
          } 
        } else {
          int startIter = 0, iter = 0;
          if(noOfByteSenderReceiveread+remainingBytes < 4 ){
            startIter = remainingBytes;
            remainingBytes += noOfByteSenderReceiveread;
            iter = 0;
          }else {
            remainingBytes = noOfByteSenderReceiveread - ((i*4)-remainingBytes);
            iter = noOfByteSenderReceiveread-remainingBytes;
          }
          for (int o = startIter; o < remainingBytes; o++){
            partialBuffer[o] = fileContents[iter++];
            countOfBytes++;
          }
          partialRecv = true;
          break;
        } 
        int powerValue = 1;
        int number = 0;
        
        //Converting the 32 bits to a number/integer.
        for(k = bits.length-1; k >= 0; k--){
          if(bits[k] == 1){
            number += powerValue;
          }
          powerValue = powerValue*2;
        }
        numbers[i++] = number;
        recvNumbers++;
      }

      //Converting Numbers to Byte array to populate file.
      int l=0, fileArrayIterator=0;
      while(l<i){
        int temp=numbers[l];
        int digit=0;
        int count=0;

        if (temp == 0) {
          count++;
          digit = 0;
          digits[digits.length - count] = 0;
        }

        //Counting the number of digits each number has.     
        while(temp > 0){
          digit = temp%10;
          temp = temp/10;
          count++;
          digits[digits.length-count] = digit;
        }

        //Push every single digit of a number as an ascii value byte to the array and write the array to file.
        for(int m = digits.length-count; m < digits.length; m++){
          fileArray[fileArrayIterator++] = (byte) (digits[m]+48);
          if(fileArrayIterator == BUFSIZ){
            //int writtenBytes = 
            fos.write(fileArray, 0, fileArrayIterator);
            //System.out.println("Bytes written into the file "+ writtenBytes);
            recvFileSize = recvFileSize+fileArrayIterator;
            fileArrayIterator = 0;
          }
        }

        // Perform the next read of bytes so that for the last number of the file a '\n'
        // is not added.
        if (l + 1 == i) {
          noOfByteSenderReceiveread = socket.getInputStream().read(fileContents, 0, BUFSIZ);
        }

        //Add linefeed character at the end of evry number except last number of the file.
        if(l < i && noOfByteSenderReceiveread != -1){
          fileArray[fileArrayIterator++] = (byte) '\n';
        }

        //If the buffer size equals the amount of numbers computed, write the file and calculate for the next packet.
        if((fileArrayIterator) == BUFSIZ || l == i-1){
          fos.write(fileArray, 0, fileArrayIterator);
          recvFileSize = recvFileSize + fileArrayIterator;
          fileArrayIterator = 0;
        }
        l++;
      }
      i = 0;
      
      if(noOfByteSenderReceiveread != -1){ 
        recvBytes = recvBytes + noOfByteSenderReceiveread;
      }
    }

    System.out.println("recv " + recvNumbers + " numbers, file " + recvFileSize + " bytes, transmit " + recvBytes + " bytes");
    fos.close();
    socket.close();
  } catch(Exception e){
    System.out.println(e);
  }
  
} /* dorecv */

} /* class SenderReceiver */
