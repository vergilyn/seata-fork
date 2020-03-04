/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.seata.discovery.loadbalance;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import io.seata.common.loader.LoadLevel;

/**
 * The type Round robin load balance.
 *
 * @author slievrly
 */
@LoadLevel(name = "RoundRobinLoadBalance", order = 1)
public class RoundRobinLoadBalance extends AbstractLoadBalance {

    private final AtomicInteger sequence = new AtomicInteger();

    @Override
    protected <T> T doSelect(List<T> invokers) {
        int length = invokers.size();
        /* vergilyn-comment, 2020-02-29 >>>>
         * getPositiveSequence() 不如写成下面这样
         * ```
         *  int sequence = SEQUENCE.getAndIncrement();
         *  // 如果达到最大值`2147483647`，继续getAndIncr `-2147483648`、`-2147483648`...
         *  sequence = sequence < 0 ? -sequence : sequence;
         * ```
         */
        return invokers.get(getPositiveSequence() % length);
    }

    /**
     * 不如直接 {@linkplain AtomicInteger#getAndIncrement()}，当达到最大值`2147483647`时，从`-2147483648`开始。
     */
    private int getPositiveSequence() {
        for (; ; ) {
            int current = sequence.get();
            int next = current >= Integer.MAX_VALUE ? 0 : current + 1;
            if (sequence.compareAndSet(current, next)) {
                return current;
            }
        }
    }

}
