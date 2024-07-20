package com.artur.youtback;

import com.artur.youtback.config.KafkaConfig;
import com.artur.objectstorage.service.ObjectStorageService;
import jakarta.transaction.Transactional;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.requestreply.ReplyingKafkaTemplate;
import org.springframework.kafka.requestreply.RequestReplyFuture;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@Transactional
@Rollback
@EmbeddedKafka(partitions = 5, topics = {
		KafkaConfig.THUMBNAIL_INPUT_TOPIC,
		KafkaConfig.VIDEO_INPUT_TOPIC,
		KafkaConfig.USER_PICTURE_INPUT_TOPIC,
		KafkaConfig.THUMBNAIL_OUTPUT_TOPIC,
		KafkaConfig.VIDEO_OUTPUT_TOPIC,
		KafkaConfig.USER_PICTURE_OUTPUT_TOPIC
})
@SpringBootTest(classes = YoutBackApplication.class)
@ActiveProfiles("dev")
public class YoutBackApplicationTests {
	public static final String TEST_VIDEO_FILE = "src/test/files/Video.mp4";
	public static final String TEST_IMAGE_FILE = "src/test/files/Image.jpg";

    @MockBean
	protected ObjectStorageService objectStorageService;
	@MockBean
	protected ReplyingKafkaTemplate<String, String, Boolean> replyingKafkaTemplate;

	@BeforeEach
	public void mock() throws Exception {
		var requestReplyFuture = new RequestReplyFuture<>();
		requestReplyFuture.complete(new ConsumerRecord<>(KafkaConfig.USER_PICTURE_OUTPUT_TOPIC, 0, 0, "", true));
		when(replyingKafkaTemplate.sendAndReceive(any(ProducerRecord.class))).thenReturn(requestReplyFuture);
		InputStream inputStream = new ByteArrayInputStream(Files.readAllBytes(Path.of(TEST_IMAGE_FILE)));
		when(objectStorageService.getObject(anyString())).thenReturn(inputStream);
	}

}
