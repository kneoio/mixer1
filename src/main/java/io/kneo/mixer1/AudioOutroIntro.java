package io.kneo.mixer1;

import javax.sound.sampled.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

public class AudioOutroIntro {

    private static final int SAMPLE_RATE = 44100;

    public static class OutroIntroSettings {
        public float outroFadeStartSeconds; // When to start fading the main song
        public float introStartDelay; // How long before intro starts (overlap with outro)
        public float introVolume; // Volume of intro track
        public float mainSongVolume; // Volume of main song
        public float fadeToVolume; // Final volume level (0.0 = silence, 0.5 = 50%, etc.)
        public int fadeCurve; // 0=linear, 1=exponential, -1=logarithmic
        public boolean autoFadeBasedOnIntro; // Auto-calculate fade based on intro length
        public float extraFadeTime; // Additional fade time beyond intro length

        public OutroIntroSettings() {
            this(20.0f, 15.0f, 1.0f, 1.0f, 0.0f, 0, true, 7.0f);
        }

        public OutroIntroSettings(float outroFadeStartSeconds, float introStartDelay,
                                  float introVolume, float mainSongVolume, float fadeToVolume,
                                  int fadeCurve, boolean autoFadeBasedOnIntro, float extraFadeTime) {
            this.outroFadeStartSeconds = outroFadeStartSeconds;
            this.introStartDelay = introStartDelay;
            this.introVolume = introVolume;
            this.mainSongVolume = mainSongVolume;
            this.fadeToVolume = fadeToVolume;
            this.fadeCurve = fadeCurve;
            this.autoFadeBasedOnIntro = autoFadeBasedOnIntro;
            this.extraFadeTime = extraFadeTime;
        }
    }

    public static void main(String[] args) {
        String mainSong = "C:/Users/justa/Music/Brimborium.wav"; // file2 from original
        String introSong = "C:/Users/justa/Music/Intro_Lumar.wav";
        String outputFile = "C:/Users/justa/Music/outro_intro_output.wav";

        OutroIntroSettings settings = new OutroIntroSettings();
        // Customize settings if needed:
        // settings.outroFadeStartSeconds = 25.0f;
        // settings.introStartDelay = 10.0f;
        // settings.fadeToVolume = 0.2f; // Fade to 20% instead of silence
        // settings.fadeCurve = 1; // Exponential fade

        try {
            createOutroIntroMix(mainSong, introSong, outputFile, settings);
            System.out.println("Outro-intro mix created successfully: " + outputFile);
            printSettings(settings);

            // ADD THE THIRD SONG
            addSongToEnd(outputFile, "C:/Users/justa/Music/06. Dina Summer - Nothing To Hide (Moderna Remix).wav", "C:/Users/justa/Music/final_complete_mix.wav");
            System.out.println("Final mix with third song created: C:/Users/justa/Music/final_complete_mix.wav");
        } catch (Exception e) {
            System.err.println("Error creating outro-intro mix: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // NEW FUNCTION - Just append a song to the end
    public static void addSongToEnd(String existingMixPath, String songToAddPath, String outputPath)
            throws IOException, UnsupportedAudioFileException {

        List<Float> existingMix = readAudioFile(existingMixPath);
        List<Float> songToAdd = readAudioFile(songToAddPath);

        // Remove silence from END of existing mix
        int endIndex = existingMix.size() - 1;
        for (int i = existingMix.size() - 1; i >= 0; i--) {
            if (Math.abs(existingMix.get(i)) > 0.01f) {
                endIndex = i;
                break;
            }
        }

        // Trim the existing mix
        List<Float> trimmedMix = new ArrayList<>(existingMix.subList(0, endIndex + 1));

        // Add the new song
        trimmedMix.addAll(songToAdd);

        writeAudioFile(trimmedMix, outputPath);
    }

    private static void printSettings(OutroIntroSettings settings) {
        System.out.println("=== Outro-Intro Parameters ===");
        System.out.println("Outro fade starts: " + settings.outroFadeStartSeconds + " seconds before end");
        System.out.println("Intro delay: " + settings.introStartDelay + " seconds");
        System.out.println("Intro volume: " + (settings.introVolume * 100) + "%");
        System.out.println("Main song volume: " + (settings.mainSongVolume * 100) + "%");
        System.out.println("Fade to volume: " + (settings.fadeToVolume * 100) + "%");
        System.out.println("Fade curve: " + (settings.fadeCurve == 0 ? "Linear" : settings.fadeCurve == 1 ? "Exponential" : "Logarithmic"));
    }

    public static void createOutroIntroMix(String mainSongPath, String introSongPath,
                                           String outputPath, OutroIntroSettings settings)
            throws IOException, UnsupportedAudioFileException {

        // Read both audio files
        List<Float> mainSongSamples = readAudioFile(mainSongPath);
        List<Float> introSamples = readAudioFile(introSongPath);

        // Apply volume to samples
        applyVolume(mainSongSamples, settings.mainSongVolume);
        applyVolume(introSamples, settings.introVolume);

        int fadeStartSample = (int) ((mainSongSamples.size() / (float)SAMPLE_RATE - settings.outroFadeStartSeconds) * SAMPLE_RATE);
        int introStartSample = mainSongSamples.size() - (int)(settings.introStartDelay * SAMPLE_RATE);

        // Ensure valid bounds
        fadeStartSample = Math.max(0, Math.min(fadeStartSample, mainSongSamples.size()));
        introStartSample = Math.max(0, Math.min(introStartSample, mainSongSamples.size()));

        // Apply outro fade to main song
        applyOutroFade(mainSongSamples, fadeStartSample, settings.fadeToVolume, settings.fadeCurve);

        // Create output samples
        List<Float> outputSamples = new ArrayList<>(mainSongSamples);

        // Add intro samples starting at specified position
        for (int i = 0; i < introSamples.size(); i++) {
            int outputIndex = introStartSample + i;

            if (outputIndex < outputSamples.size()) {
                // Mix with existing audio
                float mixed = outputSamples.get(outputIndex) + introSamples.get(i);
                outputSamples.set(outputIndex, Math.max(-1.0f, Math.min(1.0f, mixed)));
            } else {
                // Extend beyond main song
                outputSamples.add(introSamples.get(i));
            }
        }

        // Write output file
        writeAudioFile(outputSamples, outputPath);
    }

    private static void applyVolume(List<Float> samples, float volume) {
        for (int i = 0; i < samples.size(); i++) {
            samples.set(i, samples.get(i) * volume);
        }
    }

    private static void applyOutroFade(List<Float> samples, int fadeStartSample, float fadeToVolume, int fadeCurve) {
        int fadeLength = samples.size() - fadeStartSample;

        for (int i = fadeStartSample; i < samples.size(); i++) {
            float progress = (float)(i - fadeStartSample) / fadeLength;

            // Apply fade curve
            float adjustedProgress = applyFadeCurve(progress, fadeCurve);

            // Calculate volume: start at 1.0, end at fadeToVolume
            float volume = 1.0f - adjustedProgress * (1.0f - fadeToVolume);
            samples.set(i, samples.get(i) * volume);
        }
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
            ByteBuffer bb = ByteBuffer.wrap(buffer, offset, 2);
            if (format.isBigEndian()) {
                bb.order(ByteOrder.BIG_ENDIAN);
            } else {
                bb.order(ByteOrder.LITTLE_ENDIAN);
            }
            short sample = bb.getShort();
            return sample / 32768.0f;
        } else if (format.getSampleSizeInBits() == 8) {
            int sample = buffer[offset] & 0xFF;
            return (sample - 128) / 128.0f;
        }
        return 0.0f;
    }

    private static void writeAudioFile(List<Float> samples, String outputPath) throws IOException {
        AudioFormat format = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                44100.0f,
                16,
                1,
                2,
                44100.0f,
                false
        );

        try (FileOutputStream fos = new FileOutputStream(outputPath);
             BufferedOutputStream bos = new BufferedOutputStream(fos)) {

            writeWavHeader(bos, samples.size() * 2, format);

            byte[] buffer = new byte[2];
            for (float sample : samples) {
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

        out.write("RIFF".getBytes());
        writeInt(out, 36 + dataLength);
        out.write("WAVE".getBytes());

        out.write("fmt ".getBytes());
        writeInt(out, 16);
        writeShort(out, 1);
        writeShort(out, channels);
        writeInt(out, sampleRate);
        writeInt(out, byteRate);
        writeShort(out, blockAlign);
        writeShort(out, bitsPerSample);

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