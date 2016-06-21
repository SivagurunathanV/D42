import com.amazonaws.AmazonClientException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.S3ClientOptions;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.util.IOUtils;
import com.amazonaws.util.StringUtils;

import java.io.*;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;

import javax.ws.rs.core.StreamingOutput;

/**
 * Created by sivagurunathan.v on 15/06/16.
 */
@Slf4j
public class D42Util{
  public static String fileName ="test.xlsx";
  public static final String accessKey= "8C94T38VGBJQ9CB7X0FR";
  public static final String secretKey= "6t7GEH3mKoiYH/RTeNQzXUi3lpbeiIo2tXYqNCru";
 // private static final int WRITE_RATE_TO_D42 = 128;
  private static final int WRITE_RATE_TO_D42 = 5;
  private final AmazonS3 amazonS3connection;
  private final String bucketName;
  private static final String SUFFIX = "/";

  public D42Util(String hostIP,String bucketName) {
	AWSCredentials awsCredentials = new BasicAWSCredentials(accessKey,secretKey);
	ClientConfiguration clientConfiguration = new ClientConfiguration();
	clientConfiguration.setProtocol(Protocol.HTTP);
	amazonS3connection = new AmazonS3Client(awsCredentials,clientConfiguration);
	amazonS3connection.setEndpoint(hostIP); //stage endPoint "10.47.2.2")
	amazonS3connection.setS3ClientOptions(new S3ClientOptions().withPathStyleAccess(true));
	this.bucketName = bucketName;
  }



  public void getBucketsInfo(){
	List<Bucket> bucketList = amazonS3connection.listBuckets();
	bucketList.stream().forEach(bucket -> {
	  System.out.println(bucket.getName() + "\t" +
			  StringUtils.fromDate(bucket.getCreationDate()));
	});
  }

  public void createBucket(String bucketName){
	if(amazonS3connection.doesBucketExist(bucketName)) {
		amazonS3connection.createBucket(bucketName);
	}
  }

  // Step 1: Init upload request
  // Step 2: Create a list upload object
  // Step 3: Upload parts.
  // Step 4: Add to response.
  // Step 5: Complete Upload request

  public void uploadFile(String fileName, String filePath) throws AmazonClientException {
	InitiateMultipartUploadRequest initRequest = new InitiateMultipartUploadRequest(
			bucketName, filePath + fileName);
	InitiateMultipartUploadResult initResponse =
			amazonS3connection.initiateMultipartUpload(initRequest);
	List<PartETag> partETags = new ArrayList<>();
	File file = new File(filePath + fileName);
	long contentLength = file.length();
	log.info("Copying the "+fileName +" of size: "+contentLength/1024/1024 +" MB.");
	long partSize = WRITE_RATE_TO_D42 * 1024 * 1024; // Set part size to 5 MB.
	try {

	  long filePosition = 0;
	  for (int i = 1; filePosition < contentLength; i++) {
		// Last part can be less than partSize. Adjust part size.
		partSize = Math.min(partSize, (contentLength - filePosition));
		// Create request to upload a part.
		UploadPartRequest uploadRequest = new UploadPartRequest()
				.withBucketName(bucketName).withKey(fileName)
				.withUploadId(initResponse.getUploadId()).withPartNumber(i)
				.withFileOffset(filePosition)
				.withFile(file)
				.withPartSize(partSize);

		// step 4 :Upload part and add response to our list.
		partETags.add(amazonS3connection.uploadPart(uploadRequest).getPartETag());
		filePosition += partSize;
	  }
	  // Step 5: Complete.
	  CompleteMultipartUploadRequest compRequest = new
			  CompleteMultipartUploadRequest(bucketName,
			  fileName,
			  initResponse.getUploadId(),
			  partETags);
	  amazonS3connection.completeMultipartUpload(compRequest);
	} catch (AmazonClientException e) {
	  amazonS3connection.abortMultipartUpload(new AbortMultipartUploadRequest(
			  bucketName, fileName, initResponse.getUploadId()));
	  throw new AmazonClientException(e);
	}
  }

  public void getFile(String fileName,String path){
	File file = new File(path+fileName);
	file.setWritable(true,false);
	amazonS3connection.getObject(new GetObjectRequest(bucketName,fileName),file);
  }

  public S3ObjectInputStream getDownloadableAsStream(String fileName){
	S3Object s3Object = amazonS3connection.getObject(bucketName,fileName);
	return s3Object.getObjectContent();
  }

  public URL downloadAsURL(String fileName){

	GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest("siva","test.xlsx");
	return amazonS3connection.generatePresignedUrl(request);
  }

  public static <T> T executeWithRetries(Callable<T> operation, int maxRetires, int retryGapInMillis) throws Exception {

	int retryCount = 0;
	while(true) {
	  try {
		return operation.call();
	  } catch (Exception ex) {
		if(!(ex instanceof AmazonClientException) && !(ex instanceof SocketTimeoutException)) {
		  throw ex;
		}

		++retryCount;
		if(retryCount >= maxRetires) {
		  throw ex;
		}

		Thread.sleep((long)retryGapInMillis);
		log.error("[executeWithRetries] RetryCount : " + retryCount + "Failed with exception : " + ex);
	  }
	}
  }

  public S3Object uploadAsStream(InputStream inputStream, String fileName) throws IOException {

//	inputStream.reset();
	createBucket("dev");
	ByteArrayOutputStream baos = new ByteArrayOutputStream();
	byte[] buffer = new byte[1024];
	int len;
	while ((len = inputStream.read(buffer)) != -1){
	  baos.write(buffer,0,len);
	}
	baos.flush();
	InputStream i1 = new ByteArrayInputStream(baos.toByteArray());
	InputStream i2 = new ByteArrayInputStream(baos.toByteArray());
	byte[] result = DigestUtils.md5(i1);
	String streamMD5 = new String(Base64.encodeBase64(result));
	ObjectMetadata meta = new ObjectMetadata();
	meta.setContentLength(baos.size());
	meta.setContentMD5(streamMD5);
//	inputStream.reset();
	String folderName = "FY_report";
	createFolder(bucketName, folderName, amazonS3connection);
	String file = folderName + SUFFIX + fileName;
	PutObjectRequest request = new PutObjectRequest(bucketName , file,i2,meta);
	//request.setRedirectLocation("/code1/Reports");
	amazonS3connection.putObject(request);
	return amazonS3connection.getObject(bucketName,file);
  }

  public static void createFolder(String bucketName, String folderName, AmazonS3 client) {
	// create meta-data for your folder and set content-length to 0
	ObjectMetadata metadata = new ObjectMetadata();
	metadata.setContentLength(0);
	// create empty content
	InputStream emptyContent = new ByteArrayInputStream(new byte[0]);
	// create a PutObjectRequest passing the folder name suffixed by /
	PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName,
			folderName + SUFFIX, emptyContent, metadata);
	// send request to S3 to create folder
	client.putObject(putObjectRequest);
  }

}
