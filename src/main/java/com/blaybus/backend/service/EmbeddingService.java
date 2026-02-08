package com.blaybus.backend.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import com.blaybus.backend.dto.OpenAiDto.EmbeddingRequest;
import com.blaybus.backend.dto.OpenAiDto.EmbeddingResponse;
import com.blaybus.backend.exception.BusinessException;
import com.blaybus.backend.exception.CommonErrorCode;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
public class EmbeddingService {

	private final RestClient openAiRestClient;
	private final String embeddingModel;

	public EmbeddingService(
		@Qualifier("openAiRestClient")
		RestClient openAiRestClient,
		@Value("${openai.embedding-model:text-embedding-3-small}")
		String embeddingModel) {
		this.openAiRestClient = openAiRestClient;
		this.embeddingModel = embeddingModel;
	}

	public double calculateSimilarity(String text1, String text2) {
		List<Double> embedding1 = getEmbedding(text1);
		List<Double> embedding2 = getEmbedding(text2);
		return cosineSimilarity(embedding1, embedding2);
	}

	private List<Double> getEmbedding(String text) {
		EmbeddingRequest request = new EmbeddingRequest(text, embeddingModel);
		try {
			EmbeddingResponse response = openAiRestClient.post()
				.uri("/embeddings")
				.body(request)
				.retrieve()
				.body(EmbeddingResponse.class);

			if (response == null || response.data() == null || response.data().isEmpty()) {
				throw new BusinessException(CommonErrorCode.EMBEDDING_API_ERROR);
			}

			return response.data().get(0).embedding();
		} catch (RestClientException e) {
			log.error("Embedding API call failed: {}", e.getMessage());
			throw new BusinessException(CommonErrorCode.EMBEDDING_API_ERROR);
		}
	}

	private double cosineSimilarity(List<Double> vec1, List<Double> vec2) {
		double dotProduct = 0.0;
		double norm1 = 0.0;
		double norm2 = 0.0;

		for (int i = 0; i < vec1.size(); i++) {
			double v1 = vec1.get(i);
			double v2 = vec2.get(i);
			dotProduct += v1 * v2;
			norm1 += v1 * v1;
			norm2 += v2 * v2;
		}

		double denominator = Math.sqrt(norm1) * Math.sqrt(norm2);
		if (denominator == 0.0) {
			return 0.0;
		}
		return dotProduct / denominator;
	}
}
