package com.acme.ecommerce.controller;

import com.acme.ecommerce.domain.*;
import com.acme.ecommerce.exceptions.ProductNotFoundException;
import com.acme.ecommerce.service.ProductService;
import com.sun.org.apache.xpath.internal.operations.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.activation.MimetypesFileTypeMap;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.math.BigDecimal;

@Controller
@RequestMapping("/product")
@Scope("request")
public class ProductController {
	
	final Logger logger = LoggerFactory.getLogger(ProductController.class);
	
	private static final int INITIAL_PAGE = 0;
	private static final int PAGE_SIZE = 5;
	
	@Autowired
	ProductService productService;

	@Autowired
	private ShoppingCart sCart;
	
	@Autowired
	HttpSession session;
	
	@Value("${imagePath:/images/}")
	String imagePath;
	
    @RequestMapping("/")
    public String index(Model model, @RequestParam(value = "page", required = false) Integer page) {
    	logger.debug("Getting Product List");
    	logger.debug("Session ID = " + session.getId());

		Purchase purchase = sCart.getPurchase();
		CouponCode couponCode = sCart.getCouponCode();
		BigDecimal subTotal = new BigDecimal(0);
		if (purchase != null) {
			if (couponCode != null) {
				subTotal = computeSubtotal(purchase, couponCode);
			} else {
				subTotal = computeSubtotal(purchase);
			}
			model.addAttribute("subTotal", subTotal);
		}
		model.addAttribute("purchase", purchase);

		// Evaluate page. If requested parameter is null or less than 0 (to
		// prevent exception), return initial size. Otherwise, return value of
		// param. decreased by 1.
		int evalPage = (page == null || page < 1) ? INITIAL_PAGE : page - 1;
    	
    	Page<Product> products = productService.findAll(new PageRequest(evalPage, PAGE_SIZE));
    	
		model.addAttribute("products", products);

        return "index";
    }
    
    @RequestMapping(path = "/detail/{id}", method = RequestMethod.GET)
    public String productDetail(@PathVariable long id, Model model, RedirectAttributes redirectAttributes) {
    	logger.debug("Details for Product " + id);

		Purchase purchase = sCart.getPurchase();
		CouponCode couponCode = sCart.getCouponCode();
		BigDecimal subTotal = new BigDecimal(0);
		if (purchase != null) {
			subTotal = computeSubtotal(purchase);
			model.addAttribute("subTotal", subTotal);
		}
		model.addAttribute("purchase", purchase);
    	
    	Product returnProduct;
		try {
			returnProduct = productService.findById(id);
		} catch (ProductNotFoundException pnfe) {
			return "redirect:/error";
		}

    	if (returnProduct != null) {
    		model.addAttribute("product", returnProduct);
    		ProductPurchase productPurchase = new ProductPurchase();
    		productPurchase.setProduct(returnProduct);
    		productPurchase.setQuantity(1);
    		model.addAttribute("productPurchase", productPurchase);
    	} else {
    		logger.error("Product " + id + " Not Found!");
    		redirectAttributes.addFlashAttribute("message", "Sorry, we could not find that product in our database");
    		return "redirect:/error";
    	}

        return "product_detail";
    }

	@RequestMapping(path = "/error", method = RequestMethod.GET)
	public String errorView(Model model) {
		return "error";
	}
    
    @RequestMapping(path="/{id}/image", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<InputStreamResource> productImage(@PathVariable long id) throws FileNotFoundException {
    	MimetypesFileTypeMap mimeTypesMap = new MimetypesFileTypeMap();
    	
    	logger.debug("Product Image Request for " + id);
    	logger.info("Using imagePath [" + imagePath + "]");

		Product returnProduct;
		try {
			returnProduct = productService.findById(id);
		} catch (ProductNotFoundException pnfe) {
			return null;
		}

    	String imageFilePath = null;
    	if (returnProduct != null) {
    		if (!imagePath.endsWith("/")) {
    			imagePath = imagePath + "/";
    		}
    		imageFilePath = imagePath + returnProduct.getFullImageName();
    	} 
    	File imageFile = new File(imageFilePath);
    	
    	return ResponseEntity.ok()
                .contentLength(imageFile.length())
                .contentType(MediaType.parseMediaType(mimeTypesMap.getContentType(imageFile)))
                .body(new InputStreamResource(new FileInputStream(imageFile)));
    }
    
    @RequestMapping(path = "/about")
    public String aboutCartShop(Model model) {
    	logger.warn("Happy Passover! Someone actually clicked on About.");

		Purchase purchase = sCart.getPurchase();
		CouponCode couponCode = sCart.getCouponCode();
		BigDecimal subTotal = new BigDecimal(0);
		if (purchase != null) {
			subTotal = computeSubtotal(purchase);
			model.addAttribute("subTotal", subTotal);
		}
		model.addAttribute("purchase", purchase);

    	return("about");
    }

	public static BigDecimal computeSubtotal(Purchase purchase) {

		BigDecimal subTotal = new BigDecimal(0);

		for (ProductPurchase pp : purchase.getProductPurchases()) {
			//logger.debug("cart has " + pp.getQuantity() + " of " + pp.getProduct().getName() + " at " + "$" + pp.getProduct().getPrice());
			subTotal = subTotal.add(pp.getProduct().getPrice().multiply(new BigDecimal(pp.getQuantity())));
		}

		/*if (couponCode.getCode() != null && !couponCode.getCode().isEmpty()) {
			logger.info("Applying discount for coupon");
			subTotal = subTotal.multiply(new BigDecimal(0.9));
		}*/

		return subTotal;
	}

	public static BigDecimal computeSubtotal(Purchase purchase, CouponCode couponCode) {

		BigDecimal subTotal = new BigDecimal(0);

		for (ProductPurchase pp : purchase.getProductPurchases()) {
			//logger.debug("cart has " + pp.getQuantity() + " of " + pp.getProduct().getName() + " at " + "$" + pp.getProduct().getPrice());
			subTotal = subTotal.add(pp.getProduct().getPrice().multiply(new BigDecimal(pp.getQuantity())));
		}

		if (couponCode.getCode() != null && !couponCode.getCode().isEmpty()) {
			//logger.info("Applying discount for coupon");
			subTotal = subTotal.multiply(new BigDecimal(0.9));
		}

		return subTotal;
	}
}
