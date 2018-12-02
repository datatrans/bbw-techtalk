package ch.datatrans.bbwtechtalk.service;

import ch.datatrans.bbwtechtalk.controller.Basket;
import ch.datatrans.bbwtechtalk.client.DatatransClient;
import ch.datatrans.bbwtechtalk.domain.Article;
import ch.datatrans.bbwtechtalk.domain.ArticleRepository;
import ch.datatrans.bbwtechtalk.domain.Purchase;
import ch.datatrans.bbwtechtalk.domain.PurchaseRepository;
import ch.datatrans.bbwtechtalk.domain.PurchaseState;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author dominik.mengelt@gmail.com
 */
@Service
public class PurchaseService {

    private final DatatransClient datatransClient;
    private final PurchaseRepository purchaseRepository;
    private final ArticleRepository articleRepository;


    public PurchaseService(DatatransClient datatransClient,
                           PurchaseRepository purchaseRepository, ArticleRepository articleRepository) {
        this.datatransClient = datatransClient;
        this.purchaseRepository = purchaseRepository;
        this.articleRepository = articleRepository;
    }

    public String initializePurchase(Basket basket) {
        String refno = RandomStringUtils.randomAlphanumeric(20);

        // fetch the articles prices from the DB
        Article article = articleRepository.findById(basket.getArticleId()).orElseThrow(RuntimeException::new);
        BigDecimal price = article.getPrice();

        // Business logic to calculate the amount
        long priceCurrencySmallestUnit = price.scaleByPowerOfTen(price.scale()).longValue() * basket.getQuantity();

        // initialize transaction with Datatrans
        String paymentId = datatransClient.initTransaction(refno, priceCurrencySmallestUnit, "CHF");

        // crate a new purchase in DB
        Purchase purchase = new Purchase();
        purchase.setArticles(List.of(article));
        purchase.setAmount(new BigDecimal(price.longValue() * basket.getQuantity()));
        purchase.setRefno(refno);
        purchase.setState(PurchaseState.INITIALIZED);
        purchaseRepository.save(purchase);

        return paymentId;
    }

    public void updatePurchase(String refno, String transactionId, String paymentMethod) {
        Purchase purchase = purchaseRepository.findByRefno(refno);
        purchase.setState(PurchaseState.PAYED);
        purchase.setTransactionId(transactionId);
        purchase.setPaymentMethod(paymentMethod);
        purchaseRepository.save(purchase);
    }
}
