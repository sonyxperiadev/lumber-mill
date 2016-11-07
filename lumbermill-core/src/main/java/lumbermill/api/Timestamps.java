package lumbermill.api;


import java.time.Duration;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalUnit;

@SuppressWarnings("unused")
public class Timestamps {

    public final static TemporalUnit TEN_SECOND     = bucketofSize(Duration.ofSeconds(10));
    public final static TemporalUnit FIFTEEN_SECOND = bucketofSize(Duration.ofSeconds(10));
    public final static TemporalUnit THIRTY_SECOND  = bucketofSize(Duration.ofSeconds(10));

    /**
     * Creates a custom TemporalUnit that can be used to truncate timestamps into buckets
     * Before creating a custom, checkout enum java.time.temporal.ChronoUnit which contains lots of these.
     */
    public static TemporalUnit bucketofSize(Duration truncateTo) {
        return new CustomChronoUnit(truncateTo);
    }


    private static class CustomChronoUnit implements TemporalUnit {

        private final static CustomChronoUnit DAYS = new CustomChronoUnit(Duration.ofSeconds(86400));
        private final static CustomChronoUnit FOREVER = new CustomChronoUnit(Duration.ofSeconds(Long.MAX_VALUE, 999_999_999));

        private final Duration duration;

        private CustomChronoUnit(Duration estimatedDuration) {
            this.duration = estimatedDuration;
        }

        @Override
        public Duration getDuration() {
            return duration;
        }

        @Override
        public boolean isDurationEstimated() {
            return this.duration.compareTo(DAYS.duration) >= 0;
        }

        @Override
        public boolean isDateBased() {
            return this.duration.compareTo(DAYS.duration) >= 0 && this != FOREVER;
        }

        @Override
        public boolean isTimeBased() {
            return this.duration.compareTo(DAYS.duration) < 0;
        }

        @Override
        public boolean isSupportedBy(Temporal temporal) {
            return temporal.isSupported(this);
        }

        @SuppressWarnings("unchecked")
        @Override
        public <R extends Temporal> R addTo(R temporal, long amount) {
            return (R) temporal.plus(amount, this);
        }

        @Override
        public long between(Temporal temporal1Inclusive, Temporal temporal2Exclusive) {
            return temporal1Inclusive.until(temporal2Exclusive, this);
        }

        @Override
        public String toString() {
            return duration.toString();
        }

    }

}
