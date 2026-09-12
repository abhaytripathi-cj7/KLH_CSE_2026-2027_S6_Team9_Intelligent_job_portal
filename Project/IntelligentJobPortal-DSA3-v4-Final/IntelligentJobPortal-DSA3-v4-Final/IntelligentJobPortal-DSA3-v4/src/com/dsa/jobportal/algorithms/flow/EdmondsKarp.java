package com.dsa.jobportal.algorithms.flow;

import com.dsa.jobportal.structures.IntQueue;

public class EdmondsKarp {
    public static class Result {
        private final int maxFlow;
        private final int[][] flow;

        public Result(int maxFlow, int[][] flow) {
            this.maxFlow = maxFlow;
            this.flow = flow;
        }

        public int getMaxFlow() { return maxFlow; }
        public int[][] getFlow() { return flow; }
    }

    public Result compute(int[][] capacity, int source, int sink) {
        int n = capacity.length;
        int[][] residual = new int[n][n];
        int[][] flow = new int[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) residual[i][j] = capacity[i][j];
        }

        int maxFlow = 0;
        int[] parent = new int[n];
        while (bfs(residual, source, sink, parent)) {
            int pathFlow = Integer.MAX_VALUE;
            for (int v = sink; v != source; v = parent[v]) {
                int u = parent[v];
                pathFlow = Math.min(pathFlow, residual[u][v]);
            }
            for (int v = sink; v != source; v = parent[v]) {
                int u = parent[v];
                residual[u][v] -= pathFlow;
                residual[v][u] += pathFlow;
                flow[u][v] += pathFlow;
                flow[v][u] -= pathFlow;
            }
            maxFlow += pathFlow;
        }
        return new Result(maxFlow, flow);
    }

    private boolean bfs(int[][] residual, int source, int sink, int[] parent) {
        boolean[] visited = new boolean[residual.length];
        IntQueue queue = new IntQueue(residual.length + 4);
        queue.offer(source);
        visited[source] = true;
        parent[source] = -1;
        while (!queue.isEmpty()) {
            int u = queue.poll();
            for (int v = 0; v < residual.length; v++) {
                if (!visited[v] && residual[u][v] > 0) {
                    parent[v] = u;
                    visited[v] = true;
                    if (v == sink) return true;
                    queue.offer(v);
                }
            }
        }
        return false;
    }
}
