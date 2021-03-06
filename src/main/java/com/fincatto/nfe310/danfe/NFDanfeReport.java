package com.fincatto.nfe310.danfe;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import net.sf.jasperreports.engine.DefaultJasperReportsContext;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRPropertiesUtil;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.data.JRXmlDataSource;

public class NFDanfeReport {

	private final String xml;

	public NFDanfeReport(String xml) {
		this.xml = xml;
	}

	public byte[] gerarDanfeNFe(byte[] logoEmpresa) throws Exception {
		return toPDF(createJasperPrintNFe(logoEmpresa));
	}

	private static byte[] toPDF(JasperPrint print) throws JRException {
		return JasperExportManager.exportReportToPdf(print);
	}

	public JasperPrint createJasperPrintNFe(byte[] logoEmpresa) throws IOException, ParserConfigurationException, SAXException, JRException {
		try (InputStream in = NFDanfeReport.class.getClassLoader().getResourceAsStream("danfe/DANFE_NFE_RETRATO.jasper"); InputStream subDuplicatas = NFDanfeReport.class.getClassLoader().getResourceAsStream("danfe/DANFE_NFE_DUPLICATAS.jasper")) {
			final JRPropertiesUtil jrPropertiesUtil = JRPropertiesUtil.getInstance(DefaultJasperReportsContext.getInstance());
			jrPropertiesUtil.setProperty("net.sf.jasperreports.xpath.executer.factory", "net.sf.jasperreports.engine.util.xml.JaxenXPathExecuterFactory");

			Map<String, Object> parameters = new HashMap<>();
			parameters.put("SUBREPORT_DUPLICATAS", subDuplicatas);
			parameters.put("LOGO_EMPRESA", (logoEmpresa == null ? null : new ByteArrayInputStream(logoEmpresa)));

			return JasperFillManager.fillReport(in, parameters, new JRXmlDataSource(convertStringXMl2DOM(), "/nfeProc/NFe/infNFe/det"));
		}
	}

	private Document convertStringXMl2DOM() throws ParserConfigurationException, IOException, SAXException {
		try (StringReader stringReader = new StringReader(this.xml)) {
			InputSource inputSource = new InputSource();
			inputSource.setCharacterStream(stringReader);
			return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(inputSource);
		}
	}
	
	public JasperPrint createJasperPrintNFCe(byte[] logoEmpresa,String url_consulta,String qrCode, String chave,boolean homologacao,String informacoesComplementares, boolean mostrarMsgFinalizacao,List<NFCePagamento> pgtos) 
    		throws IOException, WriterException, JRException, ParserConfigurationException, SAXException {
    	
    	try (InputStream in = NFDanfeReport.class.getClassLoader().getResourceAsStream("danfe/DANFE_NFCE.jasper");
    		 InputStream subItens = NFDanfeReport.class.getClassLoader().getResourceAsStream("danfe/DANFE_NFCE_ITENS.jasper");
    		 InputStream subPagamentos = NFDanfeReport.class.getClassLoader().getResourceAsStream("danfe/DANFE_NFCE_PAGAMENTOS.jasper")) {
    		
    		Map<String, Object> parameters = new HashMap<>();
    		parameters.put("SUBREL", subItens);
    		parameters.put("SUBREL_PAGAMENTOS", subPagamentos);
    		parameters.put("PAGAMENTOS", pgtos);
    		parameters.put("QR_CODE", gerarQRCode(qrCode));
    		parameters.put("CHAVE_ACESSO_FORMATADA", formatarChaveAcesso(chave));
    		parameters.put("INFORMACOES_COMPLEMENTARES", informacoesComplementares);
    		parameters.put("MOSTRAR_MSG_FINALIZACAO", mostrarMsgFinalizacao);
    		parameters.put("URL_CONSULTA", url_consulta);
    		parameters.put("LOGO_EMPRESA", (logoEmpresa == null ? null : new ByteArrayInputStream(logoEmpresa)));
            
            return JasperFillManager.fillReport(in, parameters, new JRXmlDataSource(convertStringXMl2DOM(), "/nfeProc/NFe/infNFe/det"));
    	}
    }
	
	private String formatarChaveAcesso(String chave) {
		return StringUtils.join(chave.split("(?<=\\G....)"), " ");
	}
	
	/**
	 * Geracao do QRCode com ZXing
	 * http://repo1.maven.org/maven2/com/google/zxing/core/3.2.0/
	 */
    public BufferedImage gerarQRCode(String qrCode) throws WriterException {
    	int size = 250;
    	Map<EncodeHintType, Object> hintMap = new EnumMap<EncodeHintType, Object>(EncodeHintType.class);
    	hintMap.put(EncodeHintType.CHARACTER_SET, "UTF-8");
    	hintMap.put(EncodeHintType.MARGIN, 1); /* default = 4 */
    	hintMap.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);
     	QRCodeWriter qrCodeWriter = new QRCodeWriter();
    	BitMatrix byteMatrix = qrCodeWriter.encode(qrCode, 
    			BarcodeFormat.QR_CODE, size, size, hintMap);
    	int crunchifyWidth = byteMatrix.getWidth();
    	BufferedImage image = new BufferedImage(crunchifyWidth, crunchifyWidth, BufferedImage.TYPE_INT_RGB);
    	image.createGraphics();
     	Graphics2D graphics = (Graphics2D) image.getGraphics();
    	graphics.setColor(Color.WHITE);
    	graphics.fillRect(0, 0, crunchifyWidth, crunchifyWidth);
    	graphics.setColor(Color.BLACK);
     	for (int i = 0; i < crunchifyWidth; i++) {
    		for (int j = 0; j < crunchifyWidth; j++) {
    			if (byteMatrix.get(i, j)) {
    				graphics.fillRect(i, j, 1, 1);
    			}
    		}
    	}
    	return image;
    }

    public static class NFCePagamento {
 		private String formaPagamento;
		private BigDecimal valor;
 		public NFCePagamento(String formaPagamento, BigDecimal valor) {
			this.formaPagamento = formaPagamento;
			this.valor = valor;
		}
 		public String getFormaPagamento() {
			return formaPagamento;
		}
		public void setFormaPagamento(String formaPagamento) {
			this.formaPagamento = formaPagamento;
		}
		public BigDecimal getValor() {
			return valor;
		}
		public void setValor(BigDecimal valor) {
			this.valor = valor;
		}
	}
}
