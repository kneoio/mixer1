package io.kneo.mixer1;

import javax.sound.sampled.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

public class AudioMerger {

    public enum MixProfile {
        MANUAL,
        DJ_CROSSFADE,
        RADIO_STYLE,
        SMOOTH_BLEND,
        QUICK_CUT,
        LONG_FADE,
        OVERLAP_MIX,
        GAPLESS;

        public MixSettings getSettings() {
            return switch (this) {
                case DJ_CROSSFADE -> new MixSettings(
                        8, 0.0f, 0.0f, 1.0f, 1.0f,
                        0.0f, -1.0f, 0.0f, -1.0f, -3.0f, 1
                );
                case RADIO_STYLE -> new MixSettings(
                        0, 1.0f, 1.0f, 1.0f, 1.0f,
                        0.0f, -1.0f, 0.0f, -1.0f, 2.0f, 0
                );
                case SMOOTH_BLEND -> new MixSettings(
                        12, 0.3f, 0.3f, 1.0f, 1.0f,
                        0.0f, -1.0f, 0.0f, -1.0f, -6.0f, -1
                );
                case QUICK_CUT -> new MixSettings(
                        2, 0.0f, 0.0f, 1.0f, 1.0f,
                        0.0f, -1.0f, 0.0f, -1.0f, -10.0f, 0
                );
                case LONG_FADE -> new MixSettings(
                        20, 0.0f, 0.0f, 1.0f, 1.0f,
                        0.0f, -1.0f, 0.0f, -1.0f, -15.0f, -1
                );
                case OVERLAP_MIX -> new MixSettings(
                        15, 0.5f, 0.5f, 0.8f, 0.8f,
                        0.0f, -1.0f, 0.0f, -1.0f, -15.0f, 0
                );
                case GAPLESS -> new MixSettings(
                        0, 1.0f, 1.0f, 1.0f, 1.0f,
                        0.0f, -1.0f, 0.0f, -1.0f, 0.0f, 0
                );
                default -> new MixSettings(); // Manual - default values
            };
        }

        public String getDescription() {
            return switch (this) {
                case MANUAL -> "Manual settings - customize all parameters";
                case DJ_CROSSFADE -> "DJ-style crossfade with exponential curve";
                case RADIO_STYLE -> "Radio-style with 2-second gap, no crossfade";
                case SMOOTH_BLEND -> "Smooth logarithmic blend with partial volumes";
                case QUICK_CUT -> "Quick 2-second crossfade";
                case LONG_FADE -> "Long 20-second fade for ambient music";
                case OVERLAP_MIX -> "Both songs audible during long overlap";
                case GAPLESS -> "No gap, no crossfade - direct connection";
            };
        }
    }

    public static class MixSettings {
        public int crossfadeSeconds;
        public float song1MinVolume;
        public float song2MinVolume;
        public float song1Volume;
        public float song2Volume;
        public float song1StartTime;
        public float song1EndTime;
        public float song2StartTime;
        public float song2EndTime;
        public float gapSeconds;
        public int fadeCurve;

        public MixSettings() {
            // Default manual settings
            this(10, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, -1.0f, 0.0f, -1.0f, 0.0f, 0);
        }

        public MixSettings(int crossfadeSeconds, float song1MinVolume, float song2MinVolume,
                           float song1Volume, float song2Volume, float song1StartTime, float song1EndTime,
                           float song2StartTime, float song2EndTime, float gapSeconds, int fadeCurve) {
            this.crossfadeSeconds = crossfadeSeconds;
            this.song1MinVolume = song1MinVolume;
            this.song2MinVolume = song2MinVolume;
            this.song1Volume = song1Volume;
            this.song2Volume = song2Volume;
            this.song1StartTime = song1StartTime;
            this.song1EndTime = song1EndTime;
            this.song2StartTime = song2StartTime;
            this.song2EndTime = song2EndTime;
            this.gapSeconds = gapSeconds;
            this.fadeCurve = fadeCurve;
        }
    }

    public static void main(String[] args) {
        String file1 = "C:/Users/justa/Music/Jaded_by_Routine.wav";
        String file2 = "C:/Users/justa/Music/Brimborium.wav";
        String outputFile = "C:/Users/justa/Music/merged_output.wav";

        // Choose a profile or use MANUAL for custom settings
        MixProfile profile = MixProfile.RADIO_STYLE;
        MixSettings settings = profile.getSettings();

        // For MANUAL profile, override settings here:
        if (profile == MixProfile.MANUAL) {
            settings.crossfadeSeconds = 10;
            settings.song1MinVolume = 0.0f;
            settings.song2MinVolume = 0.0f;
            // ... set other parameters as needed
        }

        try {
            mergeAudioFiles(file1, file2, outputFile, settings);
            System.out.println("Audio files merged successfully: " + outputFile);
            System.out.println("Profile used: " + profile + " - " + profile.getDescription());
            printParameters(settings);
        } catch (Exception e) {
            System.err.println("Error merging files: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void printParameters(MixSettings settings) {
        System.out.println("=== Merge Parameters ===");
        System.out.println("Crossfade duration: " + settings.crossfadeSeconds + " seconds");
        System.out.println("Song1 min volume: " + (settings.song1MinVolume * 100) + "%");
        System.out.println("Song2 min volume: " + (settings.song2MinVolume * 100) + "%");
        System.out.println("Song1 overall volume: " + (settings.song1Volume * 100) + "%");
        System.out.println("Song2 overall volume: " + (settings.song2Volume * 100) + "%");
        System.out.println("Song1 start/end: " + settings.song1StartTime + "s / " + (settings.song1EndTime == -1 ? "full" : settings.song1EndTime + "s"));
        System.out.println("Song2 start/end: " + settings.song2StartTime + "s / " + (settings.song2EndTime == -1 ? "full" : settings.song2EndTime + "s"));
        System.out.println("Gap between songs: " + settings.gapSeconds + " seconds");
        System.out.println("Fade curve: " + (settings.fadeCurve == 0 ? "Linear" : settings.fadeCurve == 1 ? "Exponential" : "Logarithmic"));
    }

    public static void mergeAudioFiles(String file1Path, String file2Path, String outputPath, MixSettings settings)
            throws IOException, UnsupportedAudioFileException {

        // Read both audio files
        List<Float> samples1 = readAudioFile(file1Path);
        List<Float> samples2 = readAudioFile(file2Path);

        // Apply trimming and volume to samples
        samples1 = processAudioSamples(samples1, settings.song1StartTime, settings.song1EndTime, settings.song1Volume);
        samples2 = processAudioSamples(samples2, settings.song2StartTime, settings.song2EndTime, settings.song2Volume);

        // Concatenate audio files with dramatic crossfade transition
        int fadeLength = 44100 * settings.crossfadeSeconds; // Configurable crossfade duration
        int gapSamples = (int) (44100 * settings.gapSeconds); // Gap in samples
        List<Float> mergedSamples = new ArrayList<>();

        // Add all of first file
        mergedSamples.addAll(samples1);

        // Add gap or calculate overlap
        if (gapSamples > 0) {
            // Add silence gap
            for (int i = 0; i < gapSamples; i++) {
                mergedSamples.add(0.0f);
            }
            fadeLength = 0; // No crossfade with gap
        }

        // Calculate overlap start position
        int overlapStart = Math.max(0, mergedSamples.size() - fadeLength);

        // Add second file with crossfade mixing
        for (int i = 0; i < samples2.size(); i++) {
            float sample2 = samples2.get(i);
            int mergedIndex = overlapStart + i;

            if (i < fadeLength && mergedIndex < mergedSamples.size() && gapSamples <= 0) {
                // We're in the crossfade region - mix the samples
                float fadeProgress = (float) i / fadeLength; // 0.0 to 1.0

                // Apply fade curve
                float adjustedProgress = applyFadeCurve(fadeProgress, settings.fadeCurve);

                // Fade out first file (from 1.0 to song1MinVolume), fade in second file (from song2MinVolume to 1.0)
                float song1FadeVolume = 1.0f - adjustedProgress * (1.0f - settings.song1MinVolume);
                float song2FadeVolume = settings.song2MinVolume + adjustedProgress * (1.0f - settings.song2MinVolume);

                float sample1 = mergedSamples.get(mergedIndex) * song1FadeVolume;
                sample2 *= song2FadeVolume;

                // Mix both samples
                mergedSamples.set(mergedIndex, sample1 + sample2);
            } else {
                // Beyond crossfade region - just add second file samples
                mergedSamples.add(sample2);
            }
        }

        // Write merged audio to output file
        writeAudioFile(mergedSamples, outputPath);
    }

    private static List<Float> processAudioSamples(List<Float> samples, float startTime, float endTime, float volume) {
        int sampleRate = 44100;
        int startSample = (int) (startTime * sampleRate);
        int endSample = endTime == -1 ? samples.size() : (int) (endTime * sampleRate);

        // Ensure bounds
        startSample = Math.max(0, Math.min(startSample, samples.size()));
        endSample = Math.max(startSample, Math.min(endSample, samples.size()));

        // Extract and apply volume
        List<Float> processed = new ArrayList<>();
        for (int i = startSample; i < endSample; i++) {
            processed.add(samples.get(i) * volume);
        }

        return processed;
    }

    private static float applyFadeCurve(float progress, int fadeCurve) {
        switch (fadeCurve) {
            case 1: // Exponential
                return progress * progress;
            case -1: // Logarithmic
                return (float) Math.sqrt(progress);
            default: // Linear
                return progress;
        }
    }

    private static List<Float> readAudioFile(String filePath)
            throws IOException, UnsupportedAudioFileException {
        List<Float> samples = new ArrayList<>();

        try (AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(new File(filePath))) {
            AudioFormat format = audioInputStream.getFormat();
            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = audioInputStream.read(buffer)) != -1) {
                // Convert bytes to float samples
                for (int i = 0; i < bytesRead; i += format.getFrameSize()) {
                    float sample = bytesToFloat(buffer, i, format);
                    samples.add(sample);
                }
            }
        }
        return samples;
    }

    private static float bytesToFloat(byte[] buffer, int offset, AudioFormat format) {
        if (format.getSampleSizeInBits() == 16) {
            // 16-bit signed PCM
            ByteBuffer bb = ByteBuffer.wrap(buffer, offset, 2);
            if (format.isBigEndian()) {
                bb.order(ByteOrder.BIG_ENDIAN);
            } else {
                bb.order(ByteOrder.LITTLE_ENDIAN);
            }
            short sample = bb.getShort();
            return sample / 32768.0f; // Convert to -1.0 to 1.0 range
        } else if (format.getSampleSizeInBits() == 8) {
            // 8-bit unsigned PCM
            int sample = buffer[offset] & 0xFF;
            return (sample - 128) / 128.0f;
        }
        return 0.0f;
    }

    private static void writeAudioFile(List<Float> samples, String outputPath) throws IOException {
        AudioFormat format = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED, // encoding
                44100.0f,  // sample rate
                16,        // sample size in bits
                1,         // channels (mono)
                2,         // frame size
                44100.0f,  // frame rate
                false      // big endian
        );

        try (FileOutputStream fos = new FileOutputStream(outputPath);
             BufferedOutputStream bos = new BufferedOutputStream(fos)) {

            // Write WAV header
            writeWavHeader(bos, samples.size() * 2, format);

            // Write audio data
            byte[] buffer = new byte[2];
            for (float sample : samples) {
                // Convert float to 16-bit PCM
                short pcmSample = (short) (sample * 32767);
                buffer[0] = (byte) (pcmSample & 0xFF);
                buffer[1] = (byte) ((pcmSample >> 8) & 0xFF);
                bos.write(buffer);
            }
        }
    }

    private static void writeWavHeader(OutputStream out, int dataLength, AudioFormat format)
            throws IOException {
        int sampleRate = (int) format.getSampleRate();
        int channels = format.getChannels();
        int bitsPerSample = format.getSampleSizeInBits();
        int byteRate = sampleRate * channels * bitsPerSample / 8;
        int blockAlign = channels * bitsPerSample / 8;

        // RIFF header
        out.write("RIFF".getBytes());
        writeInt(out, 36 + dataLength); // File size - 8
        out.write("WAVE".getBytes());

        // fmt chunk
        out.write("fmt ".getBytes());
        writeInt(out, 16); // fmt chunk size
        writeShort(out, 1); // PCM format
        writeShort(out, channels);
        writeInt(out, sampleRate);
        writeInt(out, byteRate);
        writeShort(out, blockAlign);
        writeShort(out, bitsPerSample);

        // data chunk
        out.write("data".getBytes());
        writeInt(out, dataLength);
    }

    private static void writeInt(OutputStream out, int value) throws IOException {
        out.write(value & 0xFF);
        out.write((value >> 8) & 0xFF);
        out.write((value >> 16) & 0xFF);
        out.write((value >> 24) & 0xFF);
    }

    private static void writeShort(OutputStream out, int value) throws IOException {
        out.write(value & 0xFF);
        out.write((value >> 8) & 0xFF);
    }
}