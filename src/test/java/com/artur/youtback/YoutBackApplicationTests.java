package com.artur.youtback;

import com.artur.youtback.exception.NotFoundException;
import com.artur.youtback.repository.UserMetadataRepository;
import com.artur.youtback.service.RecommendationService;
import com.artur.youtback.service.UserService;
import com.artur.youtback.service.VideoService;
import io.minio.MinioClient;
import jakarta.transaction.Transactional;
import org.apache.kafka.clients.consumer.MockConsumer;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertFalse;

@Transactional
@Rollback
@EmbeddedKafka
@SpringBootTest(classes = YoutBackApplication.class)
@ActiveProfiles("dev")
public class YoutBackApplicationTests {
	public static final String TEST_VIDEO_FILE = "src/test/files/Video.mp4";
	public static final String TEST_IMAGE_FILE = "src/test/files/Image.jpg";

    @MockBean
    MinioClient minioClient;
	@MockBean
	protected KafkaTemplate<String, String> processingServiceTemplate;
	protected MockConsumer<String, String> mockConsumer = new MockConsumer<>(OffsetResetStrategy.EARLIEST);

	@Autowired
    UserMetadataRepository userMetadataRepository;
	@Autowired
	private UserService userService;
	@Autowired
	private VideoService videoService;
	@Autowired
    RecommendationService recommendationService;

	protected void prepopulateDatabase() throws Exception {
		userService.addUsers(2);
		videoService.addVideos(3);
	}

	@Test
	public void recommendationsTest() throws NotFoundException {
        assertFalse(recommendationService.getRecommendationsFor(null, 0, new String[]{"ru"}, 10).isEmpty());
    }




}
