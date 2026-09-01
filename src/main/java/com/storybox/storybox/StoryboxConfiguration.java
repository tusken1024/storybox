package com.storybox.storybox;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(StoryboxProperties.class)
public class StoryboxConfiguration {

}
