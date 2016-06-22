import com.amazonaws.AmazonClientException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.S3ClientOptions;
import com.amazonaws.services.s3.model.*;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by sivagurunathan.v on 16/06/16.
 */
@Slf4j
public class MultipartUpload implements Callable {

  public static String fileName;
  public static String path;
//  public static final String accessKey= "8C94T38VGBJQ9CB7X0FR"; STAGE
  public static final String accessKey= "O1J4TJ09L8Y8WDT9DSYV";
//  public static final String secretKey= "6t7GEH3mKoiYH/RTeNQzXUi3lpbeiIo2tXYqNCru"; STAGE
  public static final String secretKey= "0DBzDv1Li+ZHB3q7pZVEtigZObF1cHFgKrRUtHF0";
  // private static final int WRITE_RATE_TO_D42 = 128;
  private static final int WRITE_RATE_TO_D42 = 5;
  private final AmazonS3 amazonS3connection;
  private final String bucketName;

  public MultipartUpload(String hostIP,String bucketName,String fileName,String path) {
	AWSCredentials awsCredentials = new BasicAWSCredentials(accessKey,secretKey);
	ClientConfiguration clientConfiguration = new ClientConfiguration();
	clientConfiguration.setProtocol(Protocol.HTTP);
	amazonS3connection = new AmazonS3Client(awsCredentials,clientConfiguration);
	amazonS3connection.setEndpoint(hostIP); //stage endPoint "10.47.2.2")
	amazonS3connection.setS3ClientOptions(new S3ClientOptions().withPathStyleAccess(true));
	this.bucketName = bucketName;
	this.fileName = fileName;
	this.path = path;
  }

  @Override
  public Object call() throws Exception {
	uploadFileWithThread(fileName,path);
  	return "";
  }

  public void uploadFileWithThread(String fileName, String filePath) throws AmazonClientException {
	ExecutorService executor = Executors.newFixedThreadPool(10);
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
}
