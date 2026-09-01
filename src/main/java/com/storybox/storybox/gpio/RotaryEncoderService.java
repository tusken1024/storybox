package com.storybox.storybox.gpio;

import com.pi4j.Pi4J;
import com.pi4j.context.Context;
import com.pi4j.io.gpio.digital.*;
import com.storybox.storybox.StoryboxProperties;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/**
 * Reads the KY-040 rotary encoder via pi4j and publishes
 * {@link RotaryEvent}s to the Spring application context.
 *
 * <p>Activated only when {@code storybox.gpio.enabled=true} — this lets us
 * run Storybox on a laptop (no GPIO available) without crashing.
 *
 * <h3>How the KY-040 works</h3>
 * The encoder has two outputs (CLK and DT) that are 90° out of phase
 * (quadrature signal). By comparing their states on each CLK transition,
 * we can infer the direction of rotation.
 *
 */
@Service
@ConditionalOnProperty(name = "storybox.gpio.enabled", havingValue = "true")
public class RotaryEncoderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RotaryEncoderService.class);

    private final StoryboxProperties properties;
    private final ApplicationEventPublisher eventPublisher;

    private Context pi4j;
    private DigitalInput clk;
    private DigitalInput dt;
    private DigitalInput sw;
    private int lastClkState;

    public RotaryEncoderService(StoryboxProperties properties, ApplicationEventPublisher eventPublisher) {
        this.properties = properties;
        this.eventPublisher = eventPublisher;
    }

    @PostConstruct
    void init() {
        pi4j = Pi4J.newAutoContext();

        int clkPin = properties.gpio().clkPin();
        int dtPin = properties.gpio().dtPin();
        int swPin = properties.gpio().swPin();

        clk = pi4j.create(DigitalInput.newConfigBuilder(pi4j)
                .id("ky040-clk").address(clkPin)
                .pull(PullResistance.PULL_UP)
                .debounce(3000L)
                .build());

        dt = pi4j.create(DigitalInput.newConfigBuilder(pi4j)
                .id("ky040-dt").address(dtPin)
                .pull(PullResistance.PULL_UP)
                .debounce(3000L)
                .build());

        sw = pi4j.create(DigitalInput.newConfigBuilder(pi4j)
                .id("ky040-sw").address(swPin)
                .pull(PullResistance.PULL_UP)
                .debounce(5000L)
                .build());

        lastClkState = (int) clk.state().value();

        clk.addListener(this::onRotationEvent);
        sw.addListener(this::onClickEvent);

        LOGGER.info("KY-040 initialized — CLK=BCM{}, DT=BCM{}, SW=BCM{}", clkPin, dtPin, swPin);
    }

    @PreDestroy
    void shutdown() {
        if (pi4j != null) {
            LOGGER.info("Shutting down pi4j context");
            pi4j.shutdown();
        }
    }

    private void onRotationEvent(DigitalStateChangeEvent event) {
        int currentClk = (int) event.state().value();
        // Falling edge on CLK = one detent step on the KY-040
        if (currentClk != lastClkState && currentClk == 0) {
            Direction dir = (((int) dt.state().value()) != currentClk)  ? Direction.CW : Direction.CCW;
            LOGGER.debug("Rotary {} (clk={}, dt={})",  dir, currentClk, dt.state().value());
            eventPublisher.publishEvent(new RotaryEvent(
                    dir == Direction.CW
                            ? RotaryEvent.Direction.CW
                            : RotaryEvent.Direction.CCW));
        }
        lastClkState = currentClk;
    }

    private void onClickEvent(DigitalStateChangeEvent event) {
        if (event.state() == DigitalState.LOW) {
            LOGGER.debug("Rotary button pressed");
            eventPublisher.publishEvent(new RotaryEvent(RotaryEvent.Direction.PRESS));
        }
    }

    // Internal direction enum used only for logging clarity.
    private enum Direction { CW, CCW }
}
