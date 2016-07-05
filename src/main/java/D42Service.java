import com.amazonaws.services.s3.model.S3Object;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.util.IOUtils;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.Callable;

/**
 * Created by sivagurunathan.v on 14/06/16.
 */
@Path("/settlements/d42")
public class D42Service {

//  public static final String host = "10.47.2.2"; // STAGE
  public static final String host = "10.47.2.2";
//  public static final D42Util d42Util = new D42Util(host, "code1");
  public static final D42Util d42Util = new D42Util(host, "fy-reports");
  public static final int retryCount = 5;
  public static final int retryGap = 30;

  @GET
  @Path("/getBuckets")
  public Response getBuckets() {
	d42Util.getBucketsInfo();
	return Response.noContent().build();
  }

  @POST
  @Path("/createBucket")
//  @Produces(MediaType.APPLICATION_JSON)
//  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  public Response createBucket(Buckets buckets) {
	d42Util.createBucket(buckets.getName());
	//return Response.status(Response.Status.CREATED).type(MediaType.APPLICATION_JSON).entity(BucketResponse.builder().name("asda").build()).build();
	return Response.status(Response.Status.OK).
			entity(BucketResponse.builder().message("created").build()).build();
	//return Response.status(Response.Status.CREATED).entity(bucket.getName().toString()).build();
  }

  @POST
  @Path("/upload")
  public void upload() {
	String fileName = "9f57613f05764682_FY15-16.xlsx";
	String path = "/Users/sivagurunathan.v/D42Service/src/main/";
	//d42Util.uploadFile("9f57613f05764682_FY15-16.xlsx","/Users/sivagurunathan.v/D42Service/src/main/");
	try {
	  D42Util.executeWithRetries(new Callable() {
		public Void call() {
		  d42Util.uploadFile(fileName, path);
		  return null;
		}
	  }, retryCount,retryGap);
	} catch (Exception e) {
	  e.printStackTrace();
	}
  }

  @GET
  @Path("/download")
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  public Response download() {
	String path="/Users/sivagurunathan.v/D42Service/src/main/";
	String fileName = "test.xlsx";
	InputStream stream = d42Util.getDownloadableAsStream(fileName);
	return Response.ok(stream).header("Content-Disposition",
	"attachment; filename="+fileName).build();
// try {
//	  D42Util.executeWithRetries(new Callable<S3ObjectInputStream>(){
//		public S3ObjectInputStream call(){
//		 return d42Util.getDownloadableAsStream(fileName);
//		}
//	  },retryCount,retryGap);
//	} catch (Exception e) {
//	  e.printStackTrace();
//	}

  }


  @POST
  @Path("/uploadAsStream")
  public void uploadAsStream() throws Exception {
	String path="/Users/sivagurunathan.v/D42Service/src/main/";
	String fileName = "fy_report_seller2.xlsx";
//  	String file = "/Users/sivagurunathan.v/D42Service/src/main/WriteSheet (2).xlsx";
//  	String file = "/Users/sivagurunathan.v/D42Service/src/main/test_excel.xlsx";
  	String file = "/Users/sivagurunathan.v/D42Service/src/main/test_seller.csv";
	ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
	ExcelService service = new ExcelService();
	Workbook workbook = service.createFromTemplateUsingObject();
	StreamingOutput streamingOutput = output -> workbook.write(output);
	streamingOutput.write(byteArrayOutputStream);
//	workbook.write(byteArrayOutputStream);
	byteArrayOutputStream.close();
	byte[] bytes = byteArrayOutputStream.toByteArray();
	InputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
	try {
	  D42Util.executeWithRetries(new Callable<S3Object>(){
		public S3Object call() throws IOException {
		  InputStream input = new FileInputStream(file);
//		  S3Object uploadObject = d42Util.uploadAsStream(input,fileName);
		  S3Object uploadObject = d42Util.uploadAsStreamByte(bytes,fileName);
		  input.close();
		  return uploadObject;
		}
	  },retryCount,retryGap);
	} catch (FileNotFoundException e) {
	  e.printStackTrace();
	} catch (IOException e) {
	  e.printStackTrace();
	}

  }

  @GET
  @Path("/check")
  public void checkObject() throws URISyntaxException {
	String file = "test_seller.csv";
	d42Util.checkObjectInS3("siva",file);
	URL url = d42Util.downloadAsURL("siva",file);
	System.out.println(url.toURI().toString());
  }



}
