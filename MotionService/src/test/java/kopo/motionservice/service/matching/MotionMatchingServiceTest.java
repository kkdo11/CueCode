package kopo.motionservice.service.matching;

import kopo.motionservice.repository.document.RecordedMotionDocument;
import kopo.motionservice.service.IMotionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

public class MotionMatchingServiceTest {

    private IMotionService mockService;
    private MotionMatchingService matchingService;

    @BeforeEach
    public void setup() {
        mockService = Mockito.mock(IMotionService.class);
        matchingService = new MotionMatchingService(mockService);
    }

    @Test
    public void testDtwDistance_similarSequences() {
        // Sequence a: [ [1,0], [0.8,0.2], [0.6,0.4] ]
        List<double[]> a = new ArrayList<>();
        a.add(new double[]{1.0, 0.0});
        a.add(new double[]{0.8, 0.2});
        a.add(new double[]{0.6, 0.4});

        // Sequence b: [[1,0], [0.7,0.3], [0.5,0.5]]
        double[][] b = new double[][]{
                new double[]{1.0, 0.0},
                new double[]{0.7, 0.3},
                new double[]{0.5, 0.5}
        };

        double dist = matchingService.dtwDistance(a, b);
        assertTrue(Double.isFinite(dist));
        // Expect relatively small distance
        assertTrue(dist < 1.0, "DTW distance should be less than 1.0 for similar sequences, got " + dist);
    }

    @Test
    public void testMatchSequence_findsBest() {
        // Create two recorded motions with simple face_blendshapes
        RecordedMotionDocument docA = new RecordedMotionDocument();
        docA.setRecordId("rA");
        docA.setPhrase("HelloA");
        docA.setMotionType("face_and_hand");
        RecordedMotionDocument.MotionDataDocument mdA = new RecordedMotionDocument.MotionDataDocument();
        RecordedMotionDocument.FaceBlendshapesFrameDocument fa1 = new RecordedMotionDocument.FaceBlendshapesFrameDocument();
        fa1.setTimestampMs(0);
        Map<String, Double> valsA1 = new HashMap<>(); valsA1.put("a", 1.0); valsA1.put("b", 0.0);
        fa1.setValues(valsA1);
        RecordedMotionDocument.FaceBlendshapesFrameDocument fa2 = new RecordedMotionDocument.FaceBlendshapesFrameDocument();
        fa2.setTimestampMs(50);
        Map<String, Double> valsA2 = new HashMap<>(); valsA2.put("a", 0.6); valsA2.put("b", 0.4);
        fa2.setValues(valsA2);
        mdA.setFaceBlendshapes(Arrays.asList(fa1, fa2));
        docA.setMotionData(mdA);

        RecordedMotionDocument docB = new RecordedMotionDocument();
        docB.setRecordId("rB");
        docB.setPhrase("HelloB");
        docB.setMotionType("hand_only");
        RecordedMotionDocument.MotionDataDocument mdB = new RecordedMotionDocument.MotionDataDocument();
        RecordedMotionDocument.FaceBlendshapesFrameDocument fb1 = new RecordedMotionDocument.FaceBlendshapesFrameDocument();
        fb1.setTimestampMs(0);
        Map<String, Double> valsB1 = new HashMap<>(); valsB1.put("a", 0.0); valsB1.put("b", 1.0);
        fb1.setValues(valsB1);
        mdB.setFaceBlendshapes(Collections.singletonList(fb1));
        docB.setMotionData(mdB);

        when(mockService.getAllRecordedMotions()).thenReturn(Arrays.asList(docA, docB));

        // Reload cache
        matchingService.reloadCache();

        // Live sequence similar to docA
        List<double[]> live = new ArrayList<>();
        live.add(new double[]{1.0, 0.0});
        live.add(new double[]{0.7, 0.3});

        MotionMatchingService.MatchResult res = matchingService.matchSequence(live, "face");
        assertNotNull(res);
        assertEquals("HelloA", res.getPhrase());
        assertTrue(Double.isFinite(res.getScore()));
    }

    @Test
    public void testMatchSequence_emptyReturnsNoMatch() {
        when(mockService.getAllRecordedMotions()).thenReturn(Collections.emptyList());
        matchingService.reloadCache();
        MotionMatchingService.MatchResult res = matchingService.matchSequence(Collections.emptyList(), "face");
        assertNull(res.getRecordId());
        assertTrue(Double.isInfinite(res.getScore()));
    }
}

