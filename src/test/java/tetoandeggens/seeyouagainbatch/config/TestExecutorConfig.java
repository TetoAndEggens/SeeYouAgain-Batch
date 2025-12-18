package tetoandeggens.seeyouagainbatch.config;

import java.util.concurrent.ExecutorService;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;

@TestConfiguration
public class TestExecutorConfig {

	@Bean
	@Primary
	public ExecutorService s3UploadExecutorService() {
		return new SynchronousExecutorService();
	}

	@Bean
	public TaskExecutor s3UploadTaskExecutor() {
		return new SyncTaskExecutor();
	}

	private static class SynchronousExecutorService implements ExecutorService {
		@Override
		public void execute(Runnable command) {
			command.run();
		}

		@Override
		public void shutdown() {
		}

		@Override
		public java.util.List<Runnable> shutdownNow() {
			return java.util.Collections.emptyList();
		}

		@Override
		public boolean isShutdown() {
			return false;
		}

		@Override
		public boolean isTerminated() {
			return false;
		}

		@Override
		public boolean awaitTermination(long timeout, java.util.concurrent.TimeUnit unit) {
			return true;
		}

		@Override
		public <T> java.util.concurrent.Future<T> submit(java.util.concurrent.Callable<T> task) {
			try {
				T result = task.call();
				return java.util.concurrent.CompletableFuture.completedFuture(result);
			} catch (Exception e) {
				java.util.concurrent.CompletableFuture<T> future = new java.util.concurrent.CompletableFuture<>();
				future.completeExceptionally(e);
				return future;
			}
		}

		@Override
		public <T> java.util.concurrent.Future<T> submit(Runnable task, T result) {
			task.run();
			return java.util.concurrent.CompletableFuture.completedFuture(result);
		}

		@Override
		public java.util.concurrent.Future<?> submit(Runnable task) {
			task.run();
			return java.util.concurrent.CompletableFuture.completedFuture(null);
		}

		@Override
		public <T> java.util.List<java.util.concurrent.Future<T>> invokeAll(
			java.util.Collection<? extends java.util.concurrent.Callable<T>> tasks) {
			return tasks.stream()
				.map(this::submit)
				.collect(java.util.stream.Collectors.toList());
		}

		@Override
		public <T> java.util.List<java.util.concurrent.Future<T>> invokeAll(
			java.util.Collection<? extends java.util.concurrent.Callable<T>> tasks,
			long timeout,
			java.util.concurrent.TimeUnit unit) {
			return invokeAll(tasks);
		}

		@Override
		public <T> T invokeAny(java.util.Collection<? extends java.util.concurrent.Callable<T>> tasks) {
			return tasks.stream()
				.findFirst()
				.map(task -> {
					try {
						return task.call();
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				})
				.orElse(null);
		}

		@Override
		public <T> T invokeAny(
			java.util.Collection<? extends java.util.concurrent.Callable<T>> tasks,
			long timeout,
			java.util.concurrent.TimeUnit unit) {
			return invokeAny(tasks);
		}
	}
}