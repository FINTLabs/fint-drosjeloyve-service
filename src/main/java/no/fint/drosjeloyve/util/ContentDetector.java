package no.fint.drosjeloyve.util;

import no.fint.drosjeloyve.model.AltinnApplication;
import org.apache.commons.lang3.StringUtils;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.detect.DefaultDetector;
import org.apache.tika.detect.Detector;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.mime.MimeType;
import org.apache.tika.mime.MimeTypeException;

import java.io.IOException;

public class ContentDetector {

    public static String getMediaType(byte[] content, AltinnApplication.Attachment attachment) {
        Detector defaultDetector = new DefaultDetector();
        Metadata metadata = new Metadata();

        try {
            TikaInputStream tikaInputStream = TikaInputStream.get(content);
            MediaType mediaType = defaultDetector.detect(tikaInputStream, metadata);
            tikaInputStream.close();
            return mediaType.toString();
        } catch (IOException ex) {
            return attachment.getAttachmentType();
        }
    }

    public static String getFileName(String mediaType, AltinnApplication.Attachment attachment) {
        TikaConfig defaultConfig = TikaConfig.getDefaultConfig();

        try {
            MimeType mimeType = defaultConfig.getMimeRepository().getRegisteredMimeType(mediaType);

            if (mimeType == null) {
                return getFileName(attachment);
            }

            return attachment.getAttachmentTypeName().concat(mimeType.getExtension());
        } catch (MimeTypeException ex) {
            return getFileName(attachment);
        }
    }

    private static String getFileName(AltinnApplication.Attachment attachment) {
        String extension = StringUtils.substringAfterLast(attachment.getFileName(), ".");

        if (extension.isEmpty()) {
            return attachment.getAttachmentTypeName().concat(".taxi");
        }

        return attachment.getFileName();
    }
}
