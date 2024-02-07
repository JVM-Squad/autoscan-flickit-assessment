package org.flickit.assessment.kit.adapter.out.minio;

import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.http.Method;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.flickit.assessment.common.exception.ResourceNotFoundException;
import org.flickit.assessment.kit.application.port.out.expertgroup.UploadExpertGroupPicturePort;
import org.flickit.assessment.kit.application.port.out.kitdsl.CreateDslDownloadLinkPort;
import org.flickit.assessment.kit.application.port.out.kitdsl.UploadKitDslToFileStoragePort;
import org.flickit.assessment.kit.application.port.out.minio.LoadKitDSLJsonFilePort;
import org.flickit.assessment.kit.config.MinioConfigProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.flickit.assessment.kit.adapter.out.minio.MinioConstants.*;
import static org.flickit.assessment.kit.common.ErrorMessageKey.FILE_STORAGE_FILE_NOT_FOUND;

@Component
@AllArgsConstructor
public class MinioAdapter implements
    UploadKitDslToFileStoragePort,
    LoadKitDSLJsonFilePort,
    UploadExpertGroupPicturePort,
    CreateDslDownloadLinkPort {

    public static final String SLASH = "/";
    public static final String DOT = ".";
    private final MinioClient minioClient;
    private final MinioConfigProperties properties;

    @SneakyThrows
    @Override
    public UploadKitDslToFileStoragePort.Result uploadKitDsl(MultipartFile dslZipFile, String dslJsonFile) {
        String bucketName = properties.getDslBucketName();
        UUID uniqueObjectName = UUID.randomUUID();
        String dslFileObjectName = uniqueObjectName + DSL_FILE_NAME;
        String dslJsonObjectName = uniqueObjectName + DSL_JSON_NAME;

        InputStream zipFileInputStream = dslZipFile.getInputStream();
        writeFile(bucketName, dslFileObjectName, zipFileInputStream);

        InputStream jsonFileInputStream = new ByteArrayInputStream(dslJsonFile.getBytes());
        writeFile(bucketName, dslJsonObjectName, jsonFileInputStream);

        String dslFilePath = bucketName + SLASH + dslFileObjectName;
        String dslJsonPath = bucketName + SLASH + dslJsonObjectName;
        return new UploadKitDslToFileStoragePort.Result(dslFilePath, dslJsonPath);
    }

    @SneakyThrows
    private void writeFile(String bucketName, String fileObjectName, InputStream fileInputStream) {
        minioClient.putObject(PutObjectArgs.builder()
            .bucket(bucketName)
            .object(fileObjectName)
//            .contentType()
            .stream(fileInputStream, fileInputStream.available(), -1)
            .build());
    }

    @SneakyThrows
    @Override
    public String loadDslJson(String dslJsonFullPath) {
        String bucketName = dslJsonFullPath.substring(0, dslJsonFullPath.indexOf(SLASH));
        String objectName = dslJsonFullPath.substring(dslJsonFullPath.indexOf(SLASH));

        checkFileExistence(bucketName, objectName);

        InputStream stream = minioClient
            .getObject(GetObjectArgs.builder()
                .bucket(bucketName)
                .object(objectName)
                .build());

        return new String(stream.readAllBytes());
    }

    @SneakyThrows
    @Override
    public String uploadPicture(MultipartFile pictureFile) {
        String bucketName = properties.getAvatarBucketName();
        UUID uniqueObjectName = UUID.randomUUID();

        String postFix = "";
        if (pictureFile.getOriginalFilename() != null) {
            postFix = pictureFile.getOriginalFilename().substring(pictureFile.getOriginalFilename().indexOf(DOT));
        }
        String objectName = uniqueObjectName + PIC_FILE_NAME + postFix;
        writeFile(bucketName, objectName, pictureFile.getInputStream());
        return bucketName + SLASH + objectName;
    }

    @SneakyThrows
    @Override
    public String createDownloadLink(String filePath, Duration expiryDuration) {
        String bucketName = filePath.substring(0, filePath.indexOf(SLASH));
        String objectName = filePath.substring(filePath.indexOf(SLASH));

        checkFileExistence(bucketName, objectName);

        String downloadUrl = minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
            .bucket(bucketName)
            .object(objectName)
            //.extraQueryParams(Map.of("response-content-disposition", "attachment; name=myfile.zip"))
            .expiry((int) expiryDuration.getSeconds(), TimeUnit.SECONDS)
            .method(Method.GET)
            .build());

        return downloadUrl.replace(properties.getUrl(), properties.getApi());
    }

    @SneakyThrows
    private void checkFileExistence(String bucketName, String objectName) {
        try {
            minioClient.statObject(StatObjectArgs.builder()
                .bucket(bucketName)
                .object(objectName)
                .build());
        } catch (ErrorResponseException e) {
            throw new ResourceNotFoundException(FILE_STORAGE_FILE_NOT_FOUND);
        }
    }
}
