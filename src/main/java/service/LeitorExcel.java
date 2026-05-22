package service;

import com.github.pjfanning.xlsx.StreamingReader;
import model.IndicadorSeguranca;
import model.Municipio;
import model.OcorrenciaSeguranca;
import model.PopulacaoMunicipio;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.util.IOUtils;
import repository.LogRepository;

import java.io.FileInputStream;
import java.io.InputStream;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LeitorExcel {

    private static final Logger log = LogManager.getLogger(LeitorExcel.class);

    static {
        // Algumas planilhas (banco_seguranca_*.xlsx) possuem records internos grandes
        // que estouram o limite default de proteção do POI (100 MB). Subimos o teto
        // para que o WorkbookFactory consiga abrir o arquivo.
        IOUtils.setByteArrayMaxOverride(512 * 1024 * 1024);
    }

    public static final String EVENTO_LATROCINIO = "Roubo seguido de morte (latrocínio)";
    public static final String EVENTO_HOMICIDIO = "Homicídio doloso";
    public static final String TIPO_ROUBO_VEICULO = "Roubo de veículo";
    public static final String TIPO_FURTO_VEICULO = "Furto de veículo";

    private static final String ABA_OCORRENCIAS = "Ocorrências";

    private final LogRepository logRepo = new LogRepository();

    /**
     * Mantém a leitura original da planilha de IDHM (data_idhm.xlsx).
     */
    public List<Municipio> ler(String caminhoArquivo) {

        List<Municipio> municipios = new ArrayList<>();

        log.info("Iniciando leitura do arquivo: {}", caminhoArquivo);
        logRepo.salvar("INFO", "Iniciando leitura do arquivo " + caminhoArquivo);

        try (
                InputStream arquivo = new FileInputStream(caminhoArquivo);
                Workbook workbook = WorkbookFactory.create(arquivo)
        ) {

            Sheet sheet = workbook.getSheetAt(0);
            int contador = 0;
            for (Row row : sheet) {

                if (row.getRowNum() == 0) {
                    continue;
                }

                try {
                    String nome = row.getCell(0).getStringCellValue();
                    Double idhmGeral = row.getCell(2).getNumericCellValue();
                    Double renda = row.getCell(4).getNumericCellValue();
                    Double educacao = row.getCell(6).getNumericCellValue();
                    Double longevidade = row.getCell(8).getNumericCellValue();

                    Municipio municipio = new Municipio(nome, idhmGeral, renda, educacao, longevidade);

                    municipios.add(municipio);
                    contador++;
                    log.info("Linha {} processada: {}", row.getRowNum(), municipio);

                } catch (Exception e) {
                    log.warn("Erro ao processar linha {}: {}", row.getRowNum(), e.getMessage());
                    logRepo.salvar("WARN", "Erro ao processar a linha atual");
                }
            }

            log.info("Leitura finalizada. Total de registros: {}", municipios.size());
            logRepo.salvar("INFO", "Leitura finalizada! Total de registros: " + contador);
            System.out.println("------------------------------------------------------------------------");
        } catch (Exception e) {
            log.error("Erro ao abrir arquivo: {}", e.getMessage());
            logRepo.salvar("ERROR", "Erro ao abrir arquivo: " + e.getMessage());
        }

        return municipios;
    }

    /**
     * Lê idhm_municipios.xlsx (todos os estados).
     *
     * Layout ranking nacional (idhm_municipios.xlsx):
     *  Ranking, Município (UF), IDHM, Renda, Longevidade, Educação
     * Ou layout com coluna UF explícita.
     * Fallback legado: data_idhm.xlsx (SP).
     */
    public List<Municipio> lerIdhmMunicipios(String caminhoArquivo) {
        List<Municipio> municipios = new ArrayList<>();

        log.info("Iniciando leitura de IDHM municipal: {}", caminhoArquivo);
        logRepo.salvar("INFO", "Iniciando leitura de IDHM municipal: " + caminhoArquivo);

        try (
                InputStream arquivo = new FileInputStream(caminhoArquivo);
                Workbook workbook = WorkbookFactory.create(arquivo)
        ) {
            Sheet sheet = workbook.getSheetAt(0);
            int linhaCabecalho = detectarLinhaCabecalhoIdhm(sheet);
            Map<String, Integer> colunas = detectarColunasIdhm(sheet, linhaCabecalho);
            String layout = "legado";
            if (colunas.containsKey("_layout")) {
                int layoutCode = colunas.get("_layout");
                if (layoutCode == 1) layout = "uf";
                else if (layoutCode == 2) layout = "ranking";
            }

            int contador = 0;
            for (Row row : sheet) {
                if (row == null) continue;
                if (row.getRowNum() <= linhaCabecalho) continue;

                try {
                    String uf;
                    String nome;
                    Double idhmGeral;
                    Double renda;
                    Double educacao;
                    Double longevidade;

                    if ("legado".equals(layout)) {
                        uf = "SP";
                        nome = textoCelula(row.getCell(0));
                        idhmGeral = numeroDouble(row.getCell(2));
                        renda = numeroDouble(row.getCell(4));
                        educacao = numeroDouble(row.getCell(6));
                        longevidade = numeroDouble(row.getCell(8));
                    } else if ("ranking".equals(layout)) {
                        String nomeBruto = textoCelula(row.getCell(colunas.get("nome")));
                        uf = extrairUfDoNome(nomeBruto);
                        if (uf == null) continue;
                        nome = nomeBruto;
                        idhmGeral = numeroDouble(row.getCell(colunas.get("idhm")));
                        renda = numeroDouble(row.getCell(colunas.get("renda")));
                        longevidade = numeroDouble(row.getCell(colunas.get("longevidade")));
                        educacao = numeroDouble(row.getCell(colunas.get("educacao")));
                    } else {
                        uf = textoCelula(row.getCell(colunas.get("uf")));
                        if (uf == null) continue;
                        uf = uf.trim().toUpperCase();
                        if (uf.length() != 2) continue;

                        nome = textoCelula(row.getCell(colunas.get("nome")));
                        idhmGeral = numeroDouble(row.getCell(colunas.get("idhm")));
                        renda = numeroDouble(row.getCell(colunas.get("renda")));
                        educacao = numeroDouble(row.getCell(colunas.get("educacao")));
                        longevidade = numeroDouble(row.getCell(colunas.get("longevidade")));
                    }

                    if (nome == null || nome.isBlank()) continue;
                    nome = limparNomeMunicipio(nome);
                    if (nome.isBlank()) continue;
                    if (idhmGeral == null || idhmGeral <= 0) continue;

                    municipios.add(new Municipio(uf, nome, idhmGeral, renda, educacao, longevidade));
                    contador++;
                } catch (Exception e) {
                    log.warn("Erro ao processar linha {} de {}: {}",
                            row.getRowNum(), caminhoArquivo, e.getMessage());
                }
            }

            log.info("IDHM municipal lido: {} municípios.", contador);
            logRepo.salvar("INFO", "IDHM municipal lido: " + contador);
            System.out.println("------------------------------------------------------------------------");
        } catch (Exception e) {
            log.error("Erro ao abrir arquivo {}: {}", caminhoArquivo, e.getMessage());
            logRepo.salvar("ERROR", "Erro ao abrir arquivo " + caminhoArquivo + ": " + e.getMessage());
        }

        return municipios;
    }

    private String extrairUfDoNome(String nome) {
        if (nome == null) return null;
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("\\(([A-Z]{2})\\)\\s*$", java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(nome.trim());
        return m.find() ? m.group(1).toUpperCase() : null;
    }

    private int detectarLinhaCabecalhoIdhm(Sheet sheet) {
        int limite = Math.min(sheet.getLastRowNum(), 10);
        for (int i = 0; i <= limite; i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            for (int c = 0; c <= 8; c++) {
                String texto = normalizarHeader(textoCelula(row.getCell(c)));
                if ("uf".equals(texto) || texto.contains("municip") || texto.contains("ranking")) {
                    return i;
                }
            }
        }
        return 0;
    }

    private Map<String, Integer> detectarColunasIdhm(Sheet sheet, int linhaCabecalho) {
        Map<String, Integer> mapa = new HashMap<>();
        Row header = sheet.getRow(linhaCabecalho);
        if (header == null) {
            mapa.put("_layout", 2);
            mapa.put("nome", 1);
            mapa.put("idhm", 2);
            mapa.put("renda", 3);
            mapa.put("longevidade", 4);
            mapa.put("educacao", 5);
            return mapa;
        }

        boolean temUf = false;
        boolean temRanking = false;

        for (int c = 0; c <= header.getLastCellNum(); c++) {
            String texto = normalizarHeader(textoCelula(header.getCell(c)));
            if (texto.isEmpty()) continue;

            if ("uf".equals(texto) || texto.endsWith(" uf")) {
                mapa.put("uf", c);
                temUf = true;
            } else if (texto.contains("ranking")) {
                temRanking = true;
            } else if (texto.contains("municip") || "nome".equals(texto)) {
                mapa.put("nome", c);
            } else if (texto.contains("longev")) {
                mapa.put("longevidade", c);
            } else if (texto.contains("educ")) {
                mapa.put("educacao", c);
            } else if (texto.contains("renda")) {
                mapa.put("renda", c);
            } else if (texto.contains("idhm") && !mapa.containsKey("idhm")) {
                mapa.put("idhm", c);
            }
        }

        if (temUf) {
            mapa.put("_layout", 1);
            if (!mapa.containsKey("nome")) mapa.put("nome", mapa.get("uf") + 1);
            if (!mapa.containsKey("idhm")) mapa.put("idhm", mapa.get("nome") + 1);
            if (!mapa.containsKey("renda")) mapa.put("renda", mapa.get("idhm") + 1);
            if (!mapa.containsKey("educacao")) mapa.put("educacao", mapa.get("renda") + 1);
            if (!mapa.containsKey("longevidade")) mapa.put("longevidade", mapa.get("educacao") + 1);
            return mapa;
        }

        if (temRanking || mapa.containsKey("nome")) {
            mapa.put("_layout", 2);
            if (!mapa.containsKey("nome")) mapa.put("nome", 1);
            if (!mapa.containsKey("idhm")) mapa.put("idhm", 2);
            if (!mapa.containsKey("renda")) mapa.put("renda", 3);
            if (!mapa.containsKey("longevidade")) mapa.put("longevidade", 4);
            if (!mapa.containsKey("educacao")) mapa.put("educacao", 5);
            return mapa;
        }

        mapa.put("_layout", 0);
        return mapa;
    }

    private String normalizarHeader(String valor) {
        if (valor == null) return "";
        return Normalizer.normalize(valor.trim().toLowerCase(), Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String limparNomeMunicipio(String nome) {
        return nome.replaceAll("\\s*\\([A-Z]{2}\\)\\s*$", "").trim();
    }

    private Double numeroDouble(Cell cell) {
        if (cell == null) return null;
        try {
            switch (cell.getCellType()) {
                case NUMERIC:
                    return cell.getNumericCellValue();
                case STRING:
                    String s = cell.getStringCellValue();
                    if (s == null || s.isBlank()) return null;
                    s = s.trim().replace(",", ".");
                    return Double.parseDouble(s);
                case FORMULA:
                    try {
                        return cell.getNumericCellValue();
                    } catch (IllegalStateException ex) {
                        String s = cell.getStringCellValue();
                        if (s == null || s.isBlank()) return null;
                        s = s.trim().replace(",", ".");
                        return Double.parseDouble(s);
                    }
                default:
                    return null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Lê uma planilha banco_seguranca_YYYY.xlsx e devolve as ocorrências mensais
     * de latrocínio e homicídio doloso (mesmas categorias usadas pelo backend Node).
     *
     * Colunas esperadas:
     *  0 = UF, 1 = Município, 2 = Evento, 3 = Mês de Referência (data/serial),
     *  10 = Vítimas (preferencial) / 11 = fallback.
     */
    public List<OcorrenciaSeguranca> lerSeguranca(String caminhoArquivo, int anoRef) {
        List<OcorrenciaSeguranca> ocorrencias = new ArrayList<>();

        log.info("Iniciando leitura de segurança (streaming): {} (ano {})", caminhoArquivo, anoRef);
        logRepo.salvar("INFO", "Iniciando leitura de segurança: " + caminhoArquivo);

        // Os arquivos banco_seguranca_*.xlsx têm XML interno gigantesco (centenas de MB
        // descompactados). Usamos StreamingReader para ler linha a linha sem carregar
        // a planilha inteira na memória.
        try (
                InputStream arquivo = new FileInputStream(caminhoArquivo);
                Workbook workbook = StreamingReader.builder()
                        .rowCacheSize(100)
                        .bufferSize(8192)
                        .open(arquivo)
        ) {

            Sheet sheet = workbook.getSheet(String.valueOf(anoRef));
            if (sheet == null) {
                sheet = workbook.getSheetAt(0);
            }
            if (sheet == null) {
                log.warn("Nenhuma aba encontrada em {}", caminhoArquivo);
                return ocorrencias;
            }

            int contador = 0;
            int linhaIdx = -1;

            for (Row row : sheet) {
                linhaIdx++;
                if (linhaIdx == 0) continue;

                try {
                    String uf = textoCelula(row.getCell(0));
                    if (uf == null) continue;
                    uf = uf.trim().toUpperCase();

                    String evento = textoCelula(row.getCell(2));
                    if (!EVENTO_LATROCINIO.equals(evento) && !EVENTO_HOMICIDIO.equals(evento)) {
                        continue;
                    }

                    String nomeMunicipio = textoCelula(row.getCell(1));
                    if (nomeMunicipio == null || nomeMunicipio.isBlank()) continue;
                    nomeMunicipio = nomeMunicipio.trim();

                    int[] anoMes = extrairAnoMes(row.getCell(3));
                    if (anoMes == null) continue;
                    if (anoMes[0] != anoRef) continue;

                    int qtd = numeroInt(row.getCell(10));
                    if (qtd <= 0) qtd = numeroInt(row.getCell(11));
                    if (qtd <= 0) continue;

                    ocorrencias.add(new OcorrenciaSeguranca(
                            uf, nomeMunicipio, evento, anoMes[0], anoMes[1], qtd
                    ));
                    contador++;

                    if (contador % 50000 == 0) {
                        log.info("Segurança {}: {} ocorrências lidas até agora...", anoRef, contador);
                    }

                } catch (Exception e) {
                    log.warn("Erro ao processar linha {} de {}: {}",
                            linhaIdx, caminhoArquivo, e.getMessage());
                }
            }

            log.info("Segurança {} lida: {} ocorrências.", anoRef, contador);
            logRepo.salvar("INFO", "Segurança " + anoRef + " lida: " + contador + " ocorrências.");
            System.out.println("------------------------------------------------------------------------");
        } catch (Exception e) {
            log.error("Erro ao abrir arquivo {}: {}", caminhoArquivo, e.getMessage());
            logRepo.salvar("ERROR", "Erro ao abrir arquivo " + caminhoArquivo + ": " + e.getMessage());
        }

        return ocorrencias;
    }

    /**
     * Lê indicadores_seguranca_publica.xlsx (aba "Ocorrências") e devolve roubo
     * e furto de veículo por UF/ano.
     *
     * Colunas esperadas:
     *  0 = UF (por extenso), 1 = Tipo, 2 = Ano, 4 = Quantidade.
     */
    public List<IndicadorSeguranca> lerIndicadores(String caminhoArquivo) {
        List<IndicadorSeguranca> indicadores = new ArrayList<>();
        Map<String, IndicadorSeguranca> agregado = new HashMap<>();

        log.info("Iniciando leitura de indicadores: {}", caminhoArquivo);
        logRepo.salvar("INFO", "Iniciando leitura de indicadores: " + caminhoArquivo);

        try (
                InputStream arquivo = new FileInputStream(caminhoArquivo);
                Workbook workbook = WorkbookFactory.create(arquivo)
        ) {

            Sheet sheet = resolverAba(workbook, ABA_OCORRENCIAS);
            if (sheet == null) {
                log.warn("Aba 'Ocorrências' não encontrada em {}", caminhoArquivo);
                return indicadores;
            }

            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue;

                try {
                    String sigla = ufPorExtensoParaSigla(textoCelula(row.getCell(0)));
                    if (sigla == null) continue;

                    String tipo = textoCelula(row.getCell(1));
                    if (tipo == null) continue;
                    tipo = tipo.trim();

                    if (!TIPO_ROUBO_VEICULO.equals(tipo) && !TIPO_FURTO_VEICULO.equals(tipo)) {
                        continue;
                    }

                    int ano = numeroInt(row.getCell(2));
                    if (ano <= 0) continue;

                    int qtd = numeroInt(row.getCell(4));
                    if (qtd <= 0) continue;

                    String chave = sigla + "|" + tipo + "|" + ano;
                    IndicadorSeguranca atual = agregado.get(chave);
                    if (atual == null) {
                        agregado.put(chave, new IndicadorSeguranca(sigla, tipo, ano, qtd));
                    } else {
                        atual.setQuantidade(atual.getQuantidade() + qtd);
                    }

                } catch (Exception e) {
                    log.warn("Erro ao processar linha {} de {}: {}",
                            row.getRowNum(), caminhoArquivo, e.getMessage());
                }
            }

            indicadores.addAll(agregado.values());
            log.info("Indicadores lidos: {} registros agregados.", indicadores.size());
            logRepo.salvar("INFO", "Indicadores lidos: " + indicadores.size());
            System.out.println("------------------------------------------------------------------------");
        } catch (Exception e) {
            log.error("Erro ao abrir arquivo {}: {}", caminhoArquivo, e.getMessage());
            logRepo.salvar("ERROR", "Erro ao abrir arquivo " + caminhoArquivo + ": " + e.getMessage());
        }

        return indicadores;
    }

    /**
     * Lê populacao_municipios_2025.xls (formato XLS antigo) e devolve a população
     * estimada por município, com UF.
     *
     * Layout do IBGE (após o cabeçalho): col 0 = UF (sigla), col 3 = nome do município,
     * col 4 = população estimada.
     */
    public List<PopulacaoMunicipio> lerPopulacaoMunicipios(String caminhoArquivo, int ano) {
        List<PopulacaoMunicipio> populacoes = new ArrayList<>();

        log.info("Iniciando leitura de população municipal: {}", caminhoArquivo);
        logRepo.salvar("INFO", "Iniciando leitura de população municipal: " + caminhoArquivo);

        try (
                InputStream arquivo = new FileInputStream(caminhoArquivo);
                Workbook workbook = WorkbookFactory.create(arquivo)
        ) {

            Sheet sheet = workbook.getSheetAt(0);
            int contador = 0;
            int linhaCabecalho = detectarLinhaCabecalho(sheet);

            for (Row row : sheet) {
                if (row == null) continue;
                if (row.getRowNum() <= linhaCabecalho) continue;

                try {
                    String uf = textoCelula(row.getCell(0));
                    if (uf == null) continue;
                    uf = uf.trim().toUpperCase();
                    if (uf.length() != 2) continue;

                    String nome = textoCelula(row.getCell(3));
                    if (nome == null || nome.isBlank()) continue;
                    nome = nome.replaceAll("\\s*\\([A-Z]{2}\\)\\s*$", "").trim();

                    int pop = numeroInt(row.getCell(4));
                    if (pop <= 0) continue;

                    populacoes.add(new PopulacaoMunicipio(uf, nome, pop, ano));
                    contador++;

                } catch (Exception e) {
                    log.warn("Erro ao processar linha {} de {}: {}",
                            row.getRowNum(), caminhoArquivo, e.getMessage());
                }
            }

            log.info("População municipal lida: {} municípios.", contador);
            logRepo.salvar("INFO", "População municipal lida: " + contador);
            System.out.println("------------------------------------------------------------------------");
        } catch (Exception e) {
            log.error("Erro ao abrir arquivo {}: {}", caminhoArquivo, e.getMessage());
            logRepo.salvar("ERROR", "Erro ao abrir arquivo " + caminhoArquivo + ": " + e.getMessage());
        }

        return populacoes;
    }

    private Sheet resolverAba(Workbook workbook, String preferida) {
        if (preferida != null) {
            Sheet alvo = workbook.getSheet(preferida);
            if (alvo != null) return alvo;
        }
        if (workbook.getNumberOfSheets() == 0) return null;
        return workbook.getSheetAt(0);
    }

    private int detectarLinhaCabecalho(Sheet sheet) {
        int limite = Math.min(sheet.getLastRowNum(), 10);
        for (int i = 0; i <= limite; i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            String c0 = textoCelula(row.getCell(0));
            if (c0 != null && c0.trim().equalsIgnoreCase("UF")) {
                return i;
            }
        }
        return 0;
    }

    private String textoCelula(Cell cell) {
        if (cell == null) return null;
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                }
                double n = cell.getNumericCellValue();
                if (n == Math.floor(n) && !Double.isInfinite(n)) {
                    return String.valueOf((long) n);
                }
                return String.valueOf(n);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return cell.getStringCellValue();
                } catch (IllegalStateException ex) {
                    return String.valueOf(cell.getNumericCellValue());
                }
            default:
                return null;
        }
    }

    private int numeroInt(Cell cell) {
        if (cell == null) return 0;
        try {
            switch (cell.getCellType()) {
                case NUMERIC:
                    return (int) Math.round(cell.getNumericCellValue());
                case STRING:
                    String s = cell.getStringCellValue();
                    if (s == null || s.isBlank()) return 0;
                    s = s.trim().replace(".", "").replace(",", ".");
                    return (int) Math.round(Double.parseDouble(s));
                case FORMULA:
                    try {
                        return (int) Math.round(cell.getNumericCellValue());
                    } catch (IllegalStateException ex) {
                        return 0;
                    }
                default:
                    return 0;
            }
        } catch (Exception e) {
            return 0;
        }
    }

    private int[] extrairAnoMes(Cell cell) {
        if (cell == null) return null;
        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                if (DateUtil.isCellDateFormatted(cell)) {
                    Date data = cell.getDateCellValue();
                    if (data == null) return null;
                    @SuppressWarnings("deprecation")
                    int ano = data.getYear() + 1900;
                    @SuppressWarnings("deprecation")
                    int mes = data.getMonth() + 1;
                    return new int[]{ano, mes};
                }
                double serial = cell.getNumericCellValue();
                Date data = DateUtil.getJavaDate(serial);
                @SuppressWarnings("deprecation")
                int ano = data.getYear() + 1900;
                @SuppressWarnings("deprecation")
                int mes = data.getMonth() + 1;
                return new int[]{ano, mes};
            }
            if (cell.getCellType() == CellType.STRING) {
                String s = cell.getStringCellValue();
                if (s == null) return null;
                String[] partes = s.split("[-/]");
                if (partes.length >= 2) {
                    int ano = Integer.parseInt(partes[0].trim());
                    int mes = Integer.parseInt(partes[1].trim());
                    return new int[]{ano, mes};
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static String ufPorExtensoParaSigla(String nome) {
        if (nome == null) return null;
        String chave = Normalizer.normalize(nome.trim().toLowerCase(), Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return UFS_POR_EXTENSO.get(chave);
    }

    private static final Map<String, String> UFS_POR_EXTENSO = new HashMap<>();

    static {
        UFS_POR_EXTENSO.put("acre", "AC");
        UFS_POR_EXTENSO.put("alagoas", "AL");
        UFS_POR_EXTENSO.put("amapa", "AP");
        UFS_POR_EXTENSO.put("amazonas", "AM");
        UFS_POR_EXTENSO.put("bahia", "BA");
        UFS_POR_EXTENSO.put("ceara", "CE");
        UFS_POR_EXTENSO.put("distrito federal", "DF");
        UFS_POR_EXTENSO.put("espirito santo", "ES");
        UFS_POR_EXTENSO.put("goias", "GO");
        UFS_POR_EXTENSO.put("maranhao", "MA");
        UFS_POR_EXTENSO.put("mato grosso", "MT");
        UFS_POR_EXTENSO.put("mato grosso do sul", "MS");
        UFS_POR_EXTENSO.put("minas gerais", "MG");
        UFS_POR_EXTENSO.put("para", "PA");
        UFS_POR_EXTENSO.put("paraiba", "PB");
        UFS_POR_EXTENSO.put("parana", "PR");
        UFS_POR_EXTENSO.put("pernambuco", "PE");
        UFS_POR_EXTENSO.put("piaui", "PI");
        UFS_POR_EXTENSO.put("rio de janeiro", "RJ");
        UFS_POR_EXTENSO.put("rio grande do norte", "RN");
        UFS_POR_EXTENSO.put("rio grande do sul", "RS");
        UFS_POR_EXTENSO.put("rondonia", "RO");
        UFS_POR_EXTENSO.put("roraima", "RR");
        UFS_POR_EXTENSO.put("santa catarina", "SC");
        UFS_POR_EXTENSO.put("sao paulo", "SP");
        UFS_POR_EXTENSO.put("sergipe", "SE");
        UFS_POR_EXTENSO.put("tocantins", "TO");
    }
}
