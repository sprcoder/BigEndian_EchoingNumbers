# BigEndian_EchoingNumbers
 A socket program in java, that can encode numbers in big endian while sending via socket and decode to original numbers at the receiver.

* The file to be sent is a text file, with a number on each line.
* Each number is encoded as an unsigned 32-bit network byte order or "big-endian" as in RFC 1700.
* The encoded numbers are transmitted as raw bytes without any additional information between them.
* Receiving end decodes the number and creates the file echoing the number from the sender.

2 modes

0 - Send mode

1 - Receive mode

Both runs in different hosts.
Sender connects first and the receiver connects.

