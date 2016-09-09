package com.squareup.leakcanary;

import com.squareup.haha.perflib.RootObj;
import com.squareup.haha.perflib.Snapshot;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

import static com.squareup.haha.perflib.RootType.NATIVE_STATIC;
import static com.squareup.haha.perflib.RootType.SYSTEM_CLASS;
import static com.squareup.leakcanary.TestUtil.HeapDumpFile.BINDER_LEAK;
import static java.util.Arrays.asList;
import static junit.framework.Assert.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;

public class HeapAnalyzerTest {
  private static final ExcludedRefs NO_EXCLUDED_REFS = null;
  private static final List<RootObj> DUP_ROOTS =
          asList(new RootObj(SYSTEM_CLASS, 6L),
                  new RootObj(SYSTEM_CLASS, 5L),
                  new RootObj(SYSTEM_CLASS, 3L),
                  new RootObj(SYSTEM_CLASS, 5L),
                  new RootObj(NATIVE_STATIC, 3L));

  private HeapAnalyzer heapAnalyzer;

  @Before
  public void setUp() {
    heapAnalyzer = new HeapAnalyzer(NO_EXCLUDED_REFS);
  }

  @Test
  public void ensureUniqueRoots() {
    Snapshot snapshot = createSnapshot(DUP_ROOTS);

    heapAnalyzer.deduplicateGcRoots(snapshot);

    Collection<RootObj> uniqueRoots = snapshot.getGCRoots();
    assertThat(uniqueRoots).hasSize(4);

    List<Long> rootIds = new ArrayList<>();
    for (RootObj root : uniqueRoots) {
      rootIds.add(root.getId());
    }
    Collections.sort(rootIds);

    // 3 appears twice because even though two RootObjs have the same id, they're different types.
    assertThat(rootIds).containsExactly(3L, 3L, 5L, 6L);
  }

  private Snapshot createSnapshot(List<RootObj> gcRoots) {
    Snapshot snapshot = new Snapshot(null);
    for (RootObj root : gcRoots) {
      snapshot.addRoot(root);
    }
    return snapshot;
  }

  @Test public void binderLeak() {
    ExcludedRefs.BuilderWithParams excludedRefs =
        new ExcludedRefs.BuilderWithParams().clazz(WeakReference.class.getName())
            .alwaysExclude()
            .clazz("java.lang.ref.FinalizerReference")
            .alwaysExclude();

    AnalysisResult result = TestUtil.analyze(BINDER_LEAK, excludedRefs);

    assertTrue(result.leakFound);
  }
}
