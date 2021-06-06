/*
 * Copyright (c) 2017, Red Hat Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 *  * Neither the name of Oracle nor the names of its contributors may be used
 *    to endorse or promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.github.marschall.jcstress;

import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE;

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.Arbiter;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.LLL_Result;

import com.github.marschall.sqlid.Cache;
import com.github.marschall.sqlid.HashLruCache;


@JCStressTest
@Outcome(id = "ok1, ok2, nok3", expect = ACCEPTABLE, desc = "1 and 2 cached or 2 and 1 cached")
@Outcome(id = "ok1, nok2, nok3", expect = ACCEPTABLE, desc = "1 and 3 cached or 3 and 1 cached")
@Outcome(id = "nok1, ok2, nok3", expect = ACCEPTABLE, desc = "2 and 3 cached")
@Outcome(id = "nok1, nok2, nok3", expect = ACCEPTABLE, desc = "3 and 2 cached")
@State
public class ConcurrencyTests {

  private final Cache<String, String> cache;

  public ConcurrencyTests() {
    this.cache = new HashLruCache<>(2);
  }

  @Actor
  public void actor1() {
    this.cache.get("1", key -> "ok1");
  }

  @Actor
  public void actor2() {
    this.cache.get("2", key -> "ok2");
  }

  @Actor
  public void actor3() {
    this.cache.get("3", key -> "ok3");
  }

  @Arbiter
  public void arbiter(LLL_Result r) {
    r.r1 = this.cache.get("1", key -> "nok1");
    r.r2 = this.cache.get("2", key -> "nok2");
    r.r3 = this.cache.get("3", key -> "nok3");
  }

}
