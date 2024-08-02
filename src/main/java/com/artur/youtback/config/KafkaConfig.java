package com.artur.youtback.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.BooleanDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.requestreply.ReplyingKafkaTemplate;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {
    public static final String VIDEO_INPUT_TOPIC = "video_processor.video.input";
    public static final String VIDEO_OUTPUT_TOPIC = "video_processor.video.output";
    public static final String THUMBNAIL_INPUT_TOPIC = "video-processor.thumbnail.input";
    public static final String THUMBNAIL_OUTPUT_TOPIC = "video-processor.thumbnail.output";
    public static final String USER_PICTURE_INPUT_TOPIC = "video-processor.user-picture.input";
    public static final String USER_PICTURE_OUTPUT_TOPIC = "video-processor.user-picture.output";
    public static final String VIDEO_CREATED_NOTIFICATION_TOPIC = "video-created.notification";


    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    String bootstrapServers;

    @Bean
    public ConsumerFactory<String, Boolean> consumerFactory(){
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, BooleanDeserializer.class);
        return new DefaultKafkaConsumerFactory<>(props);
    }



    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Boolean> resultListenerFactory(ConsumerFactory<String, Boolean> consumerFactory){
        ConcurrentKafkaListenerContainerFactory<String, Boolean> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        return factory;
    }


    @Bean
    public ProducerFactory<String, String> producerFactory(){
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public ReplyingKafkaTemplate<String, String, Boolean> replyingKafkaTemplate(ProducerFactory<String, String> producerFactory,
                                                                                ConcurrentKafkaListenerContainerFactory<String, Boolean> listenerContainerFactory){
        ConcurrentMessageListenerContainer<String, Boolean> container = listenerContainerFactory.createContainer(THUMBNAIL_OUTPUT_TOPIC, VIDEO_OUTPUT_TOPIC, USER_PICTURE_OUTPUT_TOPIC);
        container.getContainerProperties().setGroupId("yout-back:consumer");
        var template = new ReplyingKafkaTemplate<>(producerFactory, container);
        listenerContainerFactory.setReplyTemplate(template);
        template.setDefaultReplyTimeout(Duration.of(5, ChronoUnit.MINUTES));
        return template;
    }


    @Bean
    public NewTopic userPictureTopicInput(){
        return TopicBuilder.name(USER_PICTURE_INPUT_TOPIC).partitions(5).build();
    }

    @Bean
    public NewTopic videoTopicInput(){
        return TopicBuilder.name(VIDEO_INPUT_TOPIC).partitions(5).build();
    }

    @Bean
    public NewTopic thumbnailTopicInput(){
        return TopicBuilder.name(THUMBNAIL_INPUT_TOPIC).partitions(5).build();
    }

    @Bean
    public NewTopic userPictureTopicOutput(){
        return TopicBuilder.name(USER_PICTURE_OUTPUT_TOPIC).partitions(5).build();
    }

    @Bean
    public NewTopic videoTopicOutput(){
        return TopicBuilder.name(VIDEO_OUTPUT_TOPIC).partitions(5).build();
    }

    @Bean
    public NewTopic thumbnailTopicOutput(){
        return TopicBuilder.name(THUMBNAIL_OUTPUT_TOPIC).partitions(5).build();
    }

    @Bean
    public NewTopic videoCreatedTopic(){
        return TopicBuilder.name(VIDEO_CREATED_NOTIFICATION_TOPIC).build();
    }
}
