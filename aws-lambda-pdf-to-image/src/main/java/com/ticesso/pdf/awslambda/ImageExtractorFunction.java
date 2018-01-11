package com.ticesso.pdf.awslambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.event.S3EventNotification;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

public class ImageExtractorFunction implements RequestHandler<SNSEvent, String> {

  @Override
  public String handleRequest(SNSEvent snsEvent, Context context) {

    final LambdaLogger logger = context.getLogger();
    final SNSEvent.SNS record = snsEvent.getRecords().get(0).getSNS();
    logger.log("Incoming request: " + record);
    logger.log("Subject: " + record.getSubject());
    logger.log("Message: " + record.getMessage());

    try {

      final Map<String, SNSEvent.MessageAttribute> attributes =
          record.getMessageAttributes();
      logger.log("MessageAttributes: " + attributes);

      final String srcBucket = attributes.get("bucket").getValue();
      String srcKey = attributes.get("key").getValue();
      // Object key may have spaces or unicode non-ASCII characters.
      srcKey = srcKey.replace('+', ' ');
      srcKey = URLDecoder.decode(srcKey, "UTF-8");

      final String dstBucket = srcBucket + "-preview";
      final String dstKey = srcKey + ".jpg";

      // Sanity check: validate that source and destination are different
      // buckets.
      if (srcBucket.equals(dstBucket)) {
        logger.log("Destination bucket must not match source bucket.");
        return "";
      }

      if (!srcKey.toLowerCase().endsWith(".pdf")) {
        logger.log("Content is no PDF: " + srcKey);
        return "";
      }

      // Download the image from S3 into a stream
      final AmazonS3 s3Client = AmazonS3ClientBuilder.defaultClient();
      final S3Object s3Object = s3Client.getObject(
          new GetObjectRequest(srcBucket, srcKey));
      final InputStream objectData = s3Object.getObjectContent();
      byte[] bytes = getImageOfPage(objectData, 0);
      final InputStream imageStream = new ByteArrayInputStream(bytes);

      // Set Content-Length and Content-Type
      final ObjectMetadata meta = new ObjectMetadata();
      meta.setContentLength(bytes.length);
      meta.setContentType("image/jpeg");

      // Uploading to S3 destination bucket
      logger.log("Writing to: " + dstBucket + "/" + dstKey);
      s3Client.putObject(dstBucket, dstKey, imageStream, meta);
      return "Ok";
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public byte[] getImageOfPage(InputStream in, int page) throws IOException {
    PDDocument document = PDDocument.load(in);
    PDFRenderer pdfRenderer = new PDFRenderer(document);
    BufferedImage image = pdfRenderer.renderImageWithDPI(page, 150, ImageType.RGB);

    ByteArrayOutputStream os = new ByteArrayOutputStream();
    ImageIO.write(image, "jpg", os);
    return os.toByteArray();
  }

}
