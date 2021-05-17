package br.com.cursojsf;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.component.html.HtmlSelectOneMenu;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.AjaxBehaviorEvent;
import javax.faces.model.SelectItem;
import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;
import javax.xml.bind.DatatypeConverter;

import com.google.gson.Gson;

import br.com.dao.DaoGeneric;
import br.com.entidades.Cidades;
import br.com.entidades.Estados;
import br.com.entidades.Pessoa;
import br.com.jpautil.JPAUtil;
import br.com.repository.IDaoPessoa;
import br.com.repository.IDaoPessoaImpl;

@ViewScoped
@ManagedBean(name = "pessoaBean")
public class PessoaBean {

	private Pessoa pessoa = new Pessoa();
	private DaoGeneric<Pessoa> daoGeneric = new DaoGeneric<Pessoa>();
	private List<Pessoa> pessoas = new ArrayList<Pessoa>();	
	private IDaoPessoa iDaoPessoa = new IDaoPessoaImpl();
	
	
	//----------------- fotos/imagens
	private Part arquivofoto;
	
	public Part getArquivofoto() {
		return arquivofoto;
	}

	public void setArquivofoto(Part arquivofoto) {
		this.arquivofoto = arquivofoto;
	}
	

	//metodo para converter a imagem de inputstream para array de bytes 
	@SuppressWarnings("unused")
	private byte[] getByte (InputStream is) throws IOException {
		
		int len;
		int size = 1024;
		byte[] buf = null;
		
		if (is instanceof ByteArrayInputStream) {
			size = is.available();
			buf = new byte[size];
			len = is.read(buf, 0, size);
		} else {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			buf = new byte[size]; 
			
			while ((len = is.read(buf, 0, size)) != -1) {
				bos.write(buf, 0, len);
			}
			
			buf = bos.toByteArray();			
		}
		
		return buf;
		
	}
	
	
	public void download() {
		Map<String, String> params = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
		String fileDownloadId = params.get("fileDownloadId");
				
	}
	
	
	
	//antes de criar na tela precisa criar o metodo conforme abaixo
	private List<SelectItem> estados;	
	public List<SelectItem> getEstados() {//cria o get para poder chamar na tela
		
		estados = iDaoPessoa.listaEstados();//passa o metodo listaEstados para o atributo estados criado acima
		
		return estados;
	}
	
	
	// criar propriedade tipo list para listar cidades
	private List<SelectItem> cidades;	
	public List<SelectItem> getCidades() {
		return cidades;
	}
	
	public void setCidades(List<SelectItem> cidades) {
		this.cidades = cidades;
	}
	
		
	
	//-----------------	
	
	
	
	
	@SuppressWarnings("unchecked")
	public void carregaCidades(AjaxBehaviorEvent event) {
		
		
		Estados estado = (Estados) ((HtmlSelectOneMenu) event.getSource()).getValue(); //pegar objeto inteiro que foi selecionado na tela
		
		
		if (estado != null) {
			pessoa.setEstados(estado);
			
			List<Cidades> cidades = JPAUtil.getEntityManager()
					.createQuery(" from Cidades where estados.id = " + estado.getId()).getResultList();
			
			List<SelectItem> selectItemsCidade = new ArrayList<SelectItem>();
			
			for (Cidades cidade : cidades) {
				selectItemsCidade.add(new SelectItem(cidade, cidade.getNome()));
			}
			
			setCidades(selectItemsCidade);
			
		}
		
	}
	
	
	//-----------------
	
	
	public String salvar() throws IOException {
		
		/* processar a imagem */
		byte[] imagemByte = getByte(arquivofoto.getInputStream());// metodo getByte criado acima
		pessoa.setFotoIconBase64Original(imagemByte); //salva a foto original
		
		//transformar em bufferimage
		BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(imagemByte));
		
		//pega o tipo da imagem
		int type = bufferedImage.getType() == 0? BufferedImage.TYPE_INT_ARGB : bufferedImage.getType();
		
		int largura = 200;
		int altura = 200;
		
		//criar a miniatura da imagem
		BufferedImage resizeImage = new BufferedImage(altura, largura, type);
		Graphics2D g = resizeImage.createGraphics();
		g.drawImage(bufferedImage, 0, 0, largura, altura, null);
		g.dispose();
		
		//escrever novamente a imagem em tamanho menor
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		String extensao = arquivofoto.getContentType().split("\\/")[1]; //image/png -- a posicao do array é pq a extensao do arquivo vem depois de image
		ImageIO.write(resizeImage, extensao, baos);
		
		String miniImagem = "data:" + arquivofoto.getContentType() + ";base64," + DatatypeConverter.printBase64Binary(baos.toByteArray());
					
		/* processar a imagem */
		
		/* processar a imagem part final */
		pessoa.setFotoIconBase64(miniImagem);
		pessoa.setExtensao(extensao);
		/* processar a imagem part final */
		
		daoGeneric.salvar(pessoa);
		pessoa = new Pessoa();	
		carregarPessoas();
		mostrarMsg("cadastrado com sucesso!");
		return "";
	}
	
	private void mostrarMsg(String msg) {
		FacesContext context = FacesContext.getCurrentInstance();
		FacesMessage message = new FacesMessage(msg);
		context.addMessage(null, message);		
	}

	public String merge() throws IOException {
					
		/* processar a imagem */
		byte[] imagemByte = getByte(arquivofoto.getInputStream());// metodo getByte criado acima
		pessoa.setFotoIconBase64Original(imagemByte); //salva a foto original
		
		//transformar em bufferimage
		BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(imagemByte));
		
		//pega o tipo da imagem
		int type = bufferedImage.getType() == 0? BufferedImage.TYPE_INT_ARGB : bufferedImage.getType();
		
		int largura = 200;
		int altura = 200;
		
		//criar a miniatura da imagem
		BufferedImage resizeImage = new BufferedImage(altura, largura, type);
		Graphics2D g = resizeImage.createGraphics();
		g.drawImage(bufferedImage, 0, 0, largura, altura, null);
		g.dispose();
		
		//escrever novamente a imagem em tamanho menor
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		String extensao = arquivofoto.getContentType().split("\\/")[1]; //image/png -- a posicao do array é pq a extensao do arquivo vem depois de image
		ImageIO.write(resizeImage, extensao, baos);
		
		String miniImagem = "data:" + arquivofoto.getContentType() + ";base64," + DatatypeConverter.printBase64Binary(baos.toByteArray());
					
		/* processar a imagem */
		
		/* processar a imagem part final */
		pessoa.setFotoIconBase64(miniImagem);
		pessoa.setExtensao(extensao);
		/* processar a imagem part final */
		
		
		pessoa = daoGeneric.merge(pessoa);
		pessoa = new Pessoa();
		mostrarMsg("atualizado com sucesso!");
		return "";
		
		
	}
	
	
	public String novo() {
		pessoa = new Pessoa();		
		
		return "";
	}
	
	
	public String remove() {
		daoGeneric.deletePorId(pessoa);
		pessoa = new Pessoa();
		carregarPessoas();
		return "";
	}
	

	
	
	
	@SuppressWarnings("unchecked")
	public void editar() {
		
		if (pessoa.getCidades() != null) {
			Estados estado = pessoa.getCidades().getEstados();
			pessoa.setEstados(estado); //carregar o estado na tela	
			
			
			//copiado do metodo carregaCidades
			List<Cidades> cidades = JPAUtil.getEntityManager()
					.createQuery(" from Cidades where estados.id = " + estado.getId()).getResultList();
			
			List<SelectItem> selectItemsCidade = new ArrayList<SelectItem>();
			
			for (Cidades cidade : cidades) {
				selectItemsCidade.add(new SelectItem(cidade, cidade.getNome()));
			}
			
			setCidades(selectItemsCidade);
			
		}
	}
	
	
	
	
	
	@PostConstruct
	public void carregarPessoas() {
		pessoas = daoGeneric.getListEntity(Pessoa.class);
	}
	

	



	public List<Pessoa> getPessoas() {
		return pessoas;
	}

	public Pessoa getPessoa() {
		return pessoa;
	}

	public void setPessoa(Pessoa pessoa) {
		this.pessoa = pessoa;
	}

	public DaoGeneric<Pessoa> getDaoGeneric() {
		return daoGeneric;
	}

	public void setDaoGeneric(DaoGeneric<Pessoa> daoGeneric) {
		this.daoGeneric = daoGeneric;
	}
	
	
	public String logar() {
		
		Pessoa pessoaUser = iDaoPessoa.consultarUsuario(pessoa.getLogin(), pessoa.getSenha());
		
		if (pessoaUser != null) {//achou usuario
			
			//adicionar o usuario na sessao usando a variavel do filter "usuarioLogado"
			FacesContext context = FacesContext.getCurrentInstance();
			ExternalContext externalContext = context.getExternalContext();
			externalContext.getSessionMap().put("usuarioLogado", pessoaUser);
			
			return "primeirapagina.jsf";
		}
				
		return "index.jsf";
	}
	
	public String deslogar() {
		
		//deslogar o usuario da sessao usando a variavel do filter "usuarioLogado"
		FacesContext context = FacesContext.getCurrentInstance();
		ExternalContext externalContext = context.getExternalContext();
		externalContext.getSessionMap().remove("usuarioLogado");
		
		HttpServletRequest httpServletRequest = (HttpServletRequest) context.getCurrentInstance().getExternalContext().getRequest();
		
		httpServletRequest.getSession().invalidate();
		
		
		return "index.jsf";
	}
	
	public boolean permiteAcesso(String acesso) {
		
		FacesContext context = FacesContext.getCurrentInstance();
		ExternalContext externalContext = context.getExternalContext();
		Pessoa pessoaUser = (Pessoa) externalContext.getSessionMap().get("usuarioLogado");	
		
		return pessoaUser.getPerfilUser().equals(acesso);
	}
	
	
	public void pesquisaCep(AjaxBehaviorEvent event) {
		
		try {
			
			URL url = new URL("https://viacep.com.br/ws/"+pessoa.getCep()+"/json/");			
			URLConnection connection = url.openConnection();
			InputStream is = connection.getInputStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
			
			String cep = ""; //variavel para armazenar os dados da api que vem do site
			StringBuilder jsonCep = new StringBuilder();
			
			while ((cep = br.readLine()) != null) {// varrer os campos e usa o stringbuilder para concatenar as informações
				jsonCep.append(cep);
			}
			
			Pessoa gsonAux = new Gson().fromJson(jsonCep.toString(), Pessoa.class); //vincula os campos json do web service com os atributos/campos da classe pessoa
			
			pessoa.setCep(gsonAux.getCep());
			pessoa.setLogradouro(gsonAux.getLogradouro());
			pessoa.setBairro(gsonAux.getBairro());
			pessoa.setComplemento(gsonAux.getComplemento());
			pessoa.setLocalidade(gsonAux.getLocalidade());
			pessoa.setUf(gsonAux.getUf());
			
			
		} catch (Exception e) {
			e.printStackTrace();
			mostrarMsg("erro ao consultar o cep");
		}
		
		
	}
	
	

}
