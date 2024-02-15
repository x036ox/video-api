package com.artur.youtback.config;

import com.artur.youtback.utils.AppConstants;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.BooleanDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

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
    public KafkaListenerContainerFactory<
                ConcurrentMessageListenerContainer<String, Boolean>
                > resultListenerFactory(ConsumerFactory<String, Boolean> consumerFactory){
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
    public KafkaTemplate<String, String> kafkaTemplate(ProducerFactory<String, String> producerFactory){
        return new KafkaTemplate<>(producerFactory);
    }


    @Bean
    public NewTopic userPictureTopicInput(){
        return TopicBuilder.name(AppConstants.USER_PICTURE_INPUT_TOPIC).build();
    }

    @Bean
    public NewTopic videoTopicInput(){
        return TopicBuilder.name(AppConstants.VIDEO_INPUT_TOPIC).build();
    }

    @Bean
    public NewTopic thumbnailTopicInput(){
        return TopicBuilder.name(AppConstants.THUMBNAIL_INPUT_TOPIC).build();
    }

    @Bean
    public NewTopic userPictureTopicOutput(){
        return TopicBuilder.name(AppConstants.USER_PICTURE_OUTPUT_TOPIC).build();
    }

    @Bean
    public NewTopic videoTopicOutput(){
        return TopicBuilder.name(AppConstants.VIDEO_OUTPUT_TOPIC).build();
    }

    @Bean
    public NewTopic thumbnailTopicOutput(){
        return TopicBuilder.name(AppConstants.THUMBNAIL_OUTPUT_TOPIC).build();
    }
}
