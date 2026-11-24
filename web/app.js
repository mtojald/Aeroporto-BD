// =====================================================================
// Interface do Aeroporto — cadastro, consultas e indicadores
// Conversa com o backend Java pela API REST em /api/...
// =====================================================================

// ---------- utilidades ----------

/** Chama a API. Envia o corpo como formulário (x-www-form-urlencoded). */
async function api(metodo, url, dados) {
  const opcoes = { method: metodo };
  if (dados) opcoes.body = new URLSearchParams(dados);
  const resp = await fetch(url, opcoes);
  const corpo = await resp.json().catch(() => ({}));
  if (!resp.ok) throw new Error(corpo.erro || `Erro ${resp.status}`);
  return corpo;
}

/** Cria um elemento HTML. Textos entram via textContent (sem risco de injeção). */
function el(tag, props = {}, ...filhos) {
  const e = document.createElement(tag);
  for (const [k, v] of Object.entries(props)) {
    if (k.startsWith('on')) e.addEventListener(k.slice(2), v);
    else if (k === 'class') e.className = v;
    else e.setAttribute(k, v);
  }
  for (const f of filhos) {
    if (f == null) continue;
    e.append(f instanceof Node ? f : document.createTextNode(String(f)));
  }
  return e;
}

let timerAviso;
function avisar(msg, erro = false) {
  const a = document.getElementById('aviso');
  a.textContent = msg;
  a.classList.toggle('erro', erro);
  a.hidden = false;
  clearTimeout(timerAviso);
  timerAviso = setTimeout(() => (a.hidden = true), erro ? 7000 : 3500);
}

const fmtData = (iso) => (iso ? iso.split('-').reverse().join('/') : '—');
const fmtHora = (h) => (h ? h.slice(0, 5) : '—');
const fmtCpf = (c) => c.replace(/(\d{3})(\d{3})(\d{3})(\d{2})/, '$1.$2.$3-$4');

const nascimentoInput = document.querySelector('[name="data_nasci"]');
if (nascimentoInput) {
  const hojeLocal = new Date();
  nascimentoInput.max = `${hojeLocal.getFullYear()}-${String(hojeLocal.getMonth()+1).padStart(2,'0')}-${String(hojeLocal.getDate()).padStart(2,'0')}`;
}

function linhaVazia(colunas, texto) {
  return el('tr', {}, el('td', { colspan: colunas, class: 'vazio' }, texto));
}

function botoesLinha(aoEditar, aoExcluir) {
  return el('td', { class: 'botoes' },
    el('button', { class: 'botao pequeno', onclick: aoEditar }, 'Editar'),
    el('button', { class: 'botao pequeno perigo', onclick: aoExcluir }, 'Excluir'));
}

/** Filtra as linhas de uma tabela pelo texto digitado na busca. */
function ligarBusca(idBusca, idTabela) {
  document.getElementById(idBusca).addEventListener('input', (ev) => {
    const termo = ev.target.value.toLowerCase();
    for (const tr of document.getElementById(idTabela).rows) {
      tr.hidden = !tr.textContent.toLowerCase().includes(termo);
    }
  });
}

/**
 * Controla um formulário que alterna entre "novo" e "editando".
 * chave = campo da chave primária (fica somente leitura na edição).
 */
function controlarFormulario({ form, titulo, chave, textoNovo, textoEditar, aoSalvar }) {
  let emEdicao = null; // valor da chave do registro sendo editado
  const cadastro = form.closest('.cadastro-recolhivel');
  const botaoCadastro = cadastro?.querySelector('.mais-cadastro');
  function abrirCadastro(aberto) {
    form.hidden = !aberto;
    cadastro?.classList.toggle('aberto',aberto);
    if (botaoCadastro) {
      botaoCadastro.textContent = aberto ? '−' : '+';
      botaoCadastro.setAttribute('aria-expanded',String(aberto));
      botaoCadastro.setAttribute('aria-label',`${aberto ? 'Fechar' : 'Abrir'} formulário de ${form.id === 'form-voo' ? 'voo' : 'passageiro'}`);
    }
  }
  botaoCadastro?.addEventListener('click',() => abrirCadastro(form.hidden));

  const modoNovo = () => {
    emEdicao = null;
    form.reset();
    form.elements[chave].readOnly = false;
    form.classList.remove('editando');
    titulo.textContent = textoNovo;
    abrirCadastro(false);
  };

  form.querySelector('[data-cancelar]').addEventListener('click', modoNovo);

  form.addEventListener('submit', async (ev) => {
    ev.preventDefault();
    const dados = Object.fromEntries(new FormData(form));
    const salvar = form.querySelector('[type=submit]');
    if (salvar.disabled) return;
    salvar.disabled = true;
    try {
      const r = await aoSalvar(dados, emEdicao);
      avisar(r.mensagem);
      modoNovo();
    } catch (e) {
      avisar(e.message, true);
    } finally {
      salvar.disabled = false;
    }
  });

  return {
    modoNovo,
    editar(valores, id) {
      abrirCadastro(true);
      emEdicao = id;
      for (const [campo, valor] of Object.entries(valores)) {
        if (form.elements[campo]) form.elements[campo].value = valor ?? '';
      }
      form.elements[chave].readOnly = true;
      form.classList.add('editando');
      titulo.textContent = textoEditar(id);
      form.scrollIntoView({ behavior: 'smooth', block: 'start' });
    },
  };
}

// =====================================================================
// PASSAGEIROS
// =====================================================================

const passageiros = (() => {
  const form = document.getElementById('form-passageiro');
  const tbody = document.getElementById('tabela-passageiros');

  const controle = controlarFormulario({
    form,
    titulo: document.getElementById('titulo-form-passageiro'),
    chave: 'cpf',
    textoNovo: 'Novo passageiro',
    textoEditar: (cpf) => `Editando passageiro ${fmtCpf(cpf)}`,
    async aoSalvar(dados, cpfEditado) {
      const r = cpfEditado
        ? await api('PUT', `/api/passageiros/${encodeURIComponent(cpfEditado)}`, dados)
        : await api('POST', '/api/passageiros', dados);
      await carregar().catch(e => avisar('Registro salvo, mas a listagem não pôde ser atualizada: ' + e.message, true));
      return r;
    },
  });

  async function excluir(p) {
    if (!confirm(`Excluir o passageiro ${p.primeiro_nome} ${p.sobrenome}?`)) return;
    try {
      avisar((await api('DELETE', `/api/passageiros/${p.cpf}`)).mensagem);
      controle.modoNovo();
      await carregar();
    } catch (e) {
      avisar(e.message, true);
    }
  }

  async function carregar() {
    let lista;
    try {
      lista = await api('GET', '/api/passageiros');
    } catch (e) {
      document.getElementById('contador-passageiros').textContent = '';
      tbody.replaceChildren(linhaVazia(7, e.message));
      throw e;
    }
    document.getElementById('contador-passageiros').textContent = `(${lista.length})`;
    if (!lista.length) {
      tbody.replaceChildren(linhaVazia(7, 'Nenhum passageiro cadastrado.'));
      return;
    }
    tbody.replaceChildren(...lista.map((p) => el('tr', {},
      el('td', {}, fmtCpf(p.cpf)),
      el('td', {}, `${p.primeiro_nome} ${p.sobrenome}`),
      el('td', {}, fmtData(p.data_nasci)),
      el('td', {}, p.email || '—'),
      el('td', {}, p.telefones || '—'),
      el('td', { class: 'num' }, p.qtd_bilhetes),
      botoesLinha(() => controle.editar(p, p.cpf), () => excluir(p)),
    )));
  }

  ligarBusca('busca-passageiros', 'tabela-passageiros');
  return { carregar };
})();

// =====================================================================
// VOOS
// =====================================================================

const voos = (() => {
  const form = document.getElementById('form-voo');
  const tbody = document.getElementById('tabela-voos');

  const controle = controlarFormulario({
    form,
    titulo: document.getElementById('titulo-form-voo'),
    chave: 'num_voo',
    textoNovo: 'Novo voo',
    textoEditar: (num) => `Editando voo ${num}`,
    async aoSalvar(dados, numEditado) {
      const r = numEditado
        ? await api('PUT', `/api/voos/${numEditado}`, dados)
        : await api('POST', '/api/voos', dados);
      await carregar();
      return r;
    },
  });

  /** Preenche os <select> das chaves estrangeiras com dados do banco. */
  async function carregarOpcoes() {
    const o = await api('GET', '/api/opcoes');
    const preencher = (nome, itens, valor, texto, vazio) => {
      const sel = form.elements[nome];
      const atual = sel.value;
      sel.replaceChildren(
        ...(vazio ? [el('option', { value: '' }, vazio)] : []),
        ...itens.map((i) => el('option', { value: valor(i) }, texto(i))));
      sel.value = atual;
    };
    const escolha = 'Selecione…';
    preencher('aeronave', o.aeronaves, (a) => a.cod_aeronave, (a) => `${a.cod_aeronave} — ${a.modelo} (${a.companhia})`, escolha);
    preencher('origem', o.aeroportos, (a) => a.cod_iata, (a) => `${a.cod_iata} — ${a.cidade}`, escolha);
    preencher('destino', o.aeroportos, (a) => a.cod_iata, (a) => `${a.cod_iata} — ${a.cidade}`, escolha);
    preencher('portao', o.portoes, (p) => `${p.num_portao}-${p.num_terminal}`, (p) => `Portão ${p.num_portao} · Terminal ${p.num_terminal}`, escolha);
    preencher('piloto', o.pilotos, (p) => p.matricula, (p) => `${p.nome} (${p.num_licenca})`, escolha);
    preencher('voo_conexao', o.voos, (v) => v.num_voo, (v) => `${v.num_voo} — ${v.origem}→${v.destino} em ${fmtData(v.data_voo)}`, '— sem conexão —');
  }

  /** Converte uma linha da listagem nos nomes de campo do formulário. */
  const paraFormulario = (v) => ({
    num_voo: v.num_voo,
    data_voo: v.data_voo,
    hora_partida: fmtHora(v.hora_partida).replace('—', ''),
    hora_chegada: fmtHora(v.hora_chegada).replace('—', ''),
    status: v.status,
    aeronave: v.cod_aeronave,
    origem: v.origem,
    destino: v.destino,
    portao: `${v.num_portao}-${v.num_terminal}`,
    piloto: v.piloto,
    voo_conexao: v.voo_conexao,
  });

  async function excluir(v) {
    if (!confirm(`Excluir o voo ${v.num_voo} (${v.origem} → ${v.destino})?\nA escala de tripulação dele também será removida.`)) return;
    try {
      avisar((await api('DELETE', `/api/voos/${v.num_voo}`)).mensagem);
      controle.modoNovo();
      await carregar();
    } catch (e) {
      avisar(e.message, true);
    }
  }

  async function carregar() {
    let lista;
    try {
      [lista] = await Promise.all([api('GET', '/api/voos'), carregarOpcoes()]);
    } catch (e) {
      document.getElementById('contador-voos').textContent = '';
      tbody.replaceChildren(linhaVazia(11, e.message));
      throw e;
    }
    document.getElementById('contador-voos').textContent = `(${lista.length})`;
    if (!lista.length) {
      tbody.replaceChildren(linhaVazia(11, 'Nenhum voo cadastrado.'));
      return;
    }
    tbody.replaceChildren(...lista.map((v) => el('tr', {},
      el('td', { class: 'num' }, v.num_voo),
      el('td', {}, fmtData(v.data_voo)),
      el('td', {}, `${fmtHora(v.hora_partida)} → ${fmtHora(v.hora_chegada)}`),
      el('td', {}, `${v.origem} → ${v.destino}`),
      el('td', {}, v.cod_aeronave, el('div', { class: 'suave' }, `${v.modelo} · ${v.companhia}`)),
      el('td', {}, v.num_portao), el('td', {}, v.num_terminal),
      el('td', {}, v.nome_piloto),
      el('td', {}, v.voo_conexao || '—'),
      el('td', {}, el('span', { class: `selo ${v.status}` }, v.status)),
      botoesLinha(() => controle.editar(paraFormulario(v), v.num_voo), () => excluir(v)),
    )));
  }

  ligarBusca('busca-voos', 'tabela-voos');
  return { carregar };
})();

// =====================================================================
// DASHBOARD 
// =====================================================================

const dashboard = (() => {
  const graficos = {}; // guarda os gráficos para destruir antes de redesenhar
  const filtros = { status: null, cia: null, ciaNome: null, inicio: null, fim: null };
  let carregamento = 0;

  const moeda = (v) => Number(v).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
  const paleta = ['#e6bb54', '#65adb0', '#87a9c6', '#d68d7c', '#b4b392', '#83b8a2', '#b6c7d5'];
  const neutra = '#dbe3e8';

  function desenhar(id, tipo, rotulos, valores, { legenda = false, cor = null, opcoes = {}, aoSelecionar = null } = {}) {
    graficos[id]?.destroy();
    const cores = cor ?? (tipo === 'doughnut' ? paleta : paleta[0]);
    const coresDado = Array.isArray(cores) ? cores : rotulos.map(() => cores);
    graficos[id] = new Chart(document.getElementById(id), {
      type: tipo,
      data: { labels: rotulos, datasets: [{ data: valores, backgroundColor: coresDado, borderColor: coresDado, borderWidth: tipo === 'line' ? 2 : 0, tension: .25 }] },
      options: {
        maintainAspectRatio: false,
        onClick: aoSelecionar ? (evento, elementos, grafico) => {
          const ponto = grafico.getElementsAtEventForMode(evento, 'nearest', { intersect: true }, true)[0];
          if (ponto) aoSelecionar(ponto.index, grafico);
        } : undefined,
        onHover: aoSelecionar ? (evento, elementos) => {
          if (evento.native?.target) evento.native.target.style.cursor = elementos.length ? 'pointer' : 'default';
        } : undefined,
        plugins: { legend: { display: legenda, position: 'bottom', labels: { usePointStyle: true, padding: 20 } } },
        ...opcoes,
      },
    });
  }

  function cartao(rotulo, valor) {
    return el('div', { class: 'indicador' }, el('span', { class: 'valor' }, valor), el('span', { class: 'rotulo' }, rotulo));
  }

  function desenharMalha(voos) {
    const area = document.getElementById('diagrama-malha');
    const aeroportos = [...new Set(voos.flatMap(v => [v.origem, v.destino]))];
    if (!aeroportos.length) { area.replaceChildren(el('p', {class:'vazio'}, 'Nenhuma conexão neste recorte.')); return; }
    const ns = 'http://www.w3.org/2000/svg';
    const svg = document.createElementNS(ns, 'svg');
    svg.setAttribute('viewBox', '0 0 640 300');
    svg.setAttribute('role','img');
    svg.setAttribute('aria-label', `Conexões entre ${aeroportos.join(', ')}`);
    const criar = (tag, atributos) => {
      const no = document.createElementNS(ns, tag);
      for (const [k,v] of Object.entries(atributos)) no.setAttribute(k,v);
      return no;
    };
    const centro = aeroportos.includes('REC') ? 'REC' : aeroportos[0];
    const outros = aeroportos.filter(a => a !== centro);
    const pontos = new Map([[centro,{x:190,y:150}]]);
    outros.forEach((a,i) => {
      const angulo = -.95 + (outros.length === 1 ? .95 : i / (outros.length - 1) * 1.9);
      pontos.set(a, {x:310 + Math.cos(angulo)*220,y:150 + Math.sin(angulo)*135});
    });
    for (const raio of [40,75,110]) svg.append(criar('circle',{cx:190,cy:150,r:raio,class:'malha-anel'}));
    const rotas = new Map();
    voos.forEach(v => { const chave=[v.origem,v.destino].sort().join('-'); rotas.set(chave,(rotas.get(chave)||0)+1); });
    for (const [chave,total] of rotas) {
      const [a,b]=chave.split('-'); const p=pontos.get(a), q=pontos.get(b);
      const path=criar('path',{d:`M ${p.x} ${p.y} Q ${(p.x+q.x)/2} ${Math.min(p.y,q.y)-30} ${q.x} ${q.y}`,class:'malha-ligacao','stroke-width':Math.min(3,1+total*.6)});
      const title=criar('title',{}); title.textContent=`${a} ↔ ${b}: ${total} voo(s)`; path.append(title);svg.append(path);
    }
    for (const [codigo,p] of pontos) {
      const grupo=criar('g',{});
      grupo.append(criar('circle',{cx:p.x,cy:p.y,r:codigo===centro?11:5,class:codigo===centro?'malha-hub':'malha-ponto'}));
      const texto=criar('text',{x:p.x+(codigo===centro?-25:14),y:p.y+(codigo===centro?37:5),class:codigo===centro?'malha-codigo principal':'malha-codigo'});
      texto.textContent=codigo;grupo.append(texto);svg.append(grupo);
    }
    area.replaceChildren(svg);
  }

  function atualizarResumo() {
    const partes = [];
    if (filtros.status) partes.push(`Status: ${filtros.status}`);
    if (filtros.cia) partes.push(`Companhia: ${filtros.ciaNome || filtros.cia}`);
    if (filtros.inicio) partes.push(`De ${fmtData(filtros.inicio)}`);
    if (filtros.fim) partes.push(`Até ${fmtData(filtros.fim)}`);
    if (filtros.dia) partes.push(`Dia: ${fmtData(filtros.dia)}`);
    if (filtros.voo) partes.push(`Voo ${filtros.voo}`);
    if (filtros.classe) partes.push(`Classe: ${filtros.classe}`);
    if (filtros.precoMin != null) partes.push(`Bilhetes: ${moeda(filtros.precoMin)} a ${moeda(filtros.precoMax)}`);
    document.getElementById('resumo-filtros').textContent = partes.length ? partes.join('  ·  ') : 'Todos os voos';
    document.getElementById('limpar-filtros').hidden = partes.length === 0;
    const nomes = {status:'Status',cia:'Companhia',inicio:'De',fim:'Até',dia:'Dia',voo:'Voo',classe:'Classe',precoMin:'Preço'};
    const selecoes = Object.keys(nomes).filter(k => filtros[k] != null && filtros[k] !== '');
    const chips = document.getElementById('selecoes-ativas');
    chips.hidden = selecoes.length === 0;
    chips.replaceChildren(...selecoes.map(k => {
      const valor = k === 'cia' ? filtros.ciaNome || filtros.cia : ['inicio','fim','dia'].includes(k) ? fmtData(filtros[k]) : k === 'precoMin' ? `${moeda(filtros.precoMin)} – ${moeda(filtros.precoMax)}` : filtros[k];
      return el('button', {class:'remover-selecao',type:'button','aria-label':`Remover filtro ${nomes[k]}: ${valor}`,onclick:() => {
        filtros[k] = null;
        if (k === 'precoMin') filtros.precoMax = null;
        if (k === 'cia') filtros.ciaNome = null;
        document.getElementById('filtro-fim').setCustomValidity('');
        atualizarResumo(); carregar().catch(mostrarErro);
      }}, `${nomes[k]}: ${valor}`, el('span',{'aria-hidden':'true'},'×'));
    }));
  }

  async function aplicar(chave, valor, rotulo, alternar = true) {
    filtros[chave] = alternar && filtros[chave] === valor ? null : (valor || null);
    if (chave === 'cia') filtros.ciaNome = filtros.cia ? rotulo : null;
    atualizarResumo();
    try { await carregar(); } catch (erro) { mostrarErro(erro); }
  }

  function mostrarErro(erro) {
    const estado = document.getElementById('estado-pagina');
    estado.hidden = false;
    estado.classList.add('erro');
    estado.textContent = erro.message || 'Não foi possível atualizar o painel.';
  }

  function carregarOpcoes(select, rotulos, valores, padrao) {
    if (select.options.length > 1) return;
    select.replaceChildren(el('option', { value: '' }, padrao), ...rotulos.map((rotulo, i) =>
      el('option', { value: valores[i] }, rotulo)));
  }

  function sincronizarControles() {
    document.getElementById('filtro-status').value = filtros.status || '';
    document.getElementById('filtro-companhia').value = filtros.cia || '';
    document.getElementById('filtro-inicio').value = filtros.inicio || '';
    document.getElementById('filtro-fim').value = filtros.fim || '';
  }

  async function carregar() {
    const versao = ++carregamento;
    try {
      const query = new URLSearchParams();
      if (filtros.status) query.set('status', filtros.status);
      if (filtros.cia) query.set('cia', filtros.cia);
      if (filtros.inicio) query.set('inicio', filtros.inicio);
      if (filtros.fim) query.set('fim', filtros.fim);
      for (const chave of ['dia', 'voo', 'classe', 'precoMin', 'precoMax']) {
        if (filtros[chave] != null) query.set(chave, filtros[chave]);
      }
      const d = await api('GET', `/api/dashboard${query.size ? `?${query}` : ''}`);
      if (versao !== carregamento) return;
      carregarOpcoes(document.getElementById('filtro-status'), d.voosPorStatus.rotulos, d.voosPorStatus.rotulos, 'Todos');
      carregarOpcoes(document.getElementById('filtro-companhia'), d.receitaPorCompanhia.rotulos, d.receitaPorCompanhia.ids || [], 'Todas');
      sincronizarControles();
      const listaVoos = await api('GET', '/api/voos');
      if (versao !== carregamento) return;
      const selecionados = new Map((d.ocupacaoPorVoo.ids || []).map((id, i) => [String(id), d.ocupacaoPorVoo.valores[i]]));
      const voosVisiveis = listaVoos.filter(v => selecionados.has(String(v.num_voo)));
      desenharMalha(voosVisiveis);
      const datas = voosVisiveis.map(v => v.data_voo).filter(Boolean).sort();
      const cancelados = voosVisiveis.filter(v => v.status === 'Cancelado').length;
      document.getElementById('contexto-operacao').replaceChildren(
        el('span', {}, datas.length ? `Período dos voos: ${fmtData(datas[0])} a ${fmtData(datas[datas.length-1])}` : 'Nenhum voo corresponde aos filtros.'),
        el('span', {class:cancelados ? 'operacao-atencao' : ''}, `${cancelados} ${cancelados === 1 ? 'voo cancelado' : 'voos cancelados'}`));
      document.getElementById('partidas-contagem').textContent = `${voosVisiveis.length} voos no recorte`;
      document.getElementById('voos-selecionados').replaceChildren(...(voosVisiveis.length ? voosVisiveis.map(v => el('tr', {},
        el('td', {}, el('button', { class: 'botao pequeno', onclick: () => aplicar('voo', String(v.num_voo)) }, v.num_voo)),
        el('td', {}, `${v.origem} → ${v.destino}`), el('td', {}, fmtData(v.data_voo)),
        el('td', {class:'hora-partida'}, fmtHora(v.hora_partida)), el('td', {}, fmtHora(v.hora_chegada)),
        el('td', {}, v.num_portao), el('td', {}, v.num_terminal),
        el('td', {}, el('span', {class:`status-partida ${v.status}`}, v.status)), el('td', {}, `${Number(selecionados.get(String(v.num_voo))).toLocaleString('pt-BR')}%`))) : [linhaVazia(9, 'Nenhum voo neste recorte.')]));
      Chart.defaults.font.family = 'Segoe UI, sans-serif';
      Chart.defaults.font.size = 13;
      Chart.defaults.borderColor = '#344650';
      Chart.defaults.color = getComputedStyle(document.body).getPropertyValue('--texto-suave') || '#5d6b7c';

      // indicadores (cards)
      const i = d.indicadores;
      document.getElementById('indicadores').replaceChildren(
        cartao('Voos', i.voos), cartao('Passageiros', i.passageiros),
        cartao('Aeronaves', i.aeronaves), cartao('Receita total', moeda(i.receita)));

      // gráficos de séries
      const statusCores = d.voosPorStatus.rotulos.map((rotulo, i) =>
        !filtros.status || filtros.status === rotulo ? paleta[i % paleta.length] : neutra);
      const ciaCores = d.receitaPorCompanhia.rotulos.map((_, i) =>
        !filtros.cia || filtros.cia === d.receitaPorCompanhia.ids?.[i] ? paleta[0] : neutra);
      desenhar('grafico-status', 'doughnut', d.voosPorStatus.rotulos, d.voosPorStatus.valores, {
        legenda: true, cor: statusCores,
        aoSelecionar: (i, grafico) => aplicar('status', grafico.data.labels[i]),
      });
      desenhar('grafico-receita', 'bar', d.receitaPorCompanhia.rotulos, d.receitaPorCompanhia.valores, {
        cor: ciaCores,
        aoSelecionar: (i, grafico) => aplicar('cia', d.receitaPorCompanhia.ids[i], grafico.data.labels[i]),
      });
      desenhar('grafico-dia', 'line', d.voosPorDia.rotulos, d.voosPorDia.valores,
               { aoSelecionar: i => aplicar('dia', d.voosPorDia.ids[i]), opcoes: { scales: { y: { beginAtZero: true, ticks: { precision: 0 } } } } });
      desenhar('grafico-ocupacao', 'bar', d.ocupacaoPorVoo.rotulos, d.ocupacaoPorVoo.valores,
               { aoSelecionar: i => aplicar('voo', d.ocupacaoPorVoo.ids[i]), cor: paleta[1], opcoes: { scales: { y: { beginAtZero: true, max: 100, ticks: { callback: v => `${v}%` } } } } });
      desenhar('grafico-classe', 'doughnut', d.bilhetesPorClasse.rotulos, d.bilhetesPorClasse.valores,
               { aoSelecionar: i => aplicar('classe', d.bilhetesPorClasse.rotulos[i]), legenda: true, cor: d.bilhetesPorClasse.rotulos.map((_, i) => paleta[i % paleta.length]) });

      // estatística descritiva + histograma do preço
      const e = d.estatisticasPreco;
      document.getElementById('estatisticas').replaceChildren(
        cartao('Bilhetes na amostra', e.n), cartao('Média', moeda(e.media ?? 0)), cartao('Mediana', moeda(e.mediana ?? 0)),
        cartao('Moda', e.moda?.length ? e.moda.map(moeda).join(' · ') : 'Sem moda'),
        cartao('Variância (R$²)', Number(e.variancia ?? 0).toLocaleString('pt-BR')),
        cartao('Desvio padrão', moeda(e.desvioPadrao ?? 0)), cartao('Mínimo', moeda(e.minimo ?? 0)), cartao('Máximo', moeda(e.maximo ?? 0)));
      desenhar('grafico-histograma', 'bar', d.histogramaPreco.rotulos, d.histogramaPreco.valores,
               { aoSelecionar: i => {
                   const intervalo = d.histogramaPreco.intervalos[i];
                   filtros.precoMax = filtros.precoMin === intervalo[0] ? null : intervalo[1];
                   aplicar('precoMin', intervalo[0]);
                 }, cor: paleta[2], opcoes: { scales: { y: { beginAtZero: true, ticks: { precision: 0 }, title: { display: true, text: 'Frequência' } },
                                                    x: { title: { display: true, text: 'Faixa de preço' } } } } });
    } catch (e) {
      if (versao !== carregamento) return;
      document.getElementById('indicadores').replaceChildren();
      document.getElementById('estatisticas').replaceChildren();
      Object.values(graficos).forEach(g => g.destroy());
      Object.keys(graficos).forEach(k => delete graficos[k]);
      throw e;
    }
  }

  document.getElementById('limpar-filtros').addEventListener('click', () => {
    filtros.status = null;
    filtros.cia = null;
    filtros.ciaNome = null;
    filtros.inicio = null;
    filtros.fim = null;
    for (const id of ['filtro-inicio','filtro-fim']) {
      const campo = document.getElementById(id); campo.value = ''; campo.setCustomValidity(''); campo.removeAttribute('aria-invalid');
    }
    document.getElementById('erro-periodo').hidden = true;
    for (const chave of ['dia', 'voo', 'classe', 'precoMin', 'precoMax']) filtros[chave] = null;
    atualizarResumo();
    carregar().catch(mostrarErro);
  });
  document.getElementById('filtro-status').addEventListener('change', (evento) =>
    aplicar('status', evento.target.value, null, false));
  document.getElementById('filtro-companhia').addEventListener('change', (evento) => {
    const nome = evento.target.selectedOptions[0]?.textContent;
    aplicar('cia', evento.target.value, nome, false);
  });

  const inicioPeriodo = document.getElementById('filtro-inicio');
  const fimPeriodo = document.getElementById('filtro-fim');
  const erroPeriodo = document.getElementById('erro-periodo');
  for (const campo of [inicioPeriodo,fimPeriodo]) campo.addEventListener('input',() => {
    campo.setCustomValidity(''); campo.removeAttribute('aria-invalid'); erroPeriodo.hidden = true;
  });
  document.getElementById('aplicar-periodo').addEventListener('click',() => {
    for (const campo of [inicioPeriodo,fimPeriodo]) {
      campo.setCustomValidity(''); campo.removeAttribute('aria-invalid');
      if (!campo.validity.valid || (campo.value && !/^\d{4}-\d{2}-\d{2}$/.test(campo.value))) {
        erroPeriodo.textContent = 'Informe uma data válida, com ano entre 1900 e 2100.';
        erroPeriodo.hidden = false; campo.setAttribute('aria-invalid','true'); campo.focus(); return;
      }
    }
    if (inicioPeriodo.value && fimPeriodo.value && inicioPeriodo.value > fimPeriodo.value) {
      erroPeriodo.textContent = 'A data final deve ser igual ou posterior à data inicial.';
      erroPeriodo.hidden = false; fimPeriodo.setAttribute('aria-invalid','true'); fimPeriodo.focus(); return;
    }
    erroPeriodo.hidden = true;
    filtros.inicio = inicioPeriodo.value || null;
    filtros.fim = fimPeriodo.value || null;
    atualizarResumo(); carregar().catch(mostrarErro);
  });

  atualizarResumo();
  return { carregar };
})();

// =====================================================================
// CONSULTAS  (GET /api/consultas -> texto do SQL + resultado)
// =====================================================================

const consultas = (() => {
  async function carregar() {
    const lista = document.getElementById('lista-consultas');
    try {
      const dados = await api('GET', '/api/consultas');
      document.getElementById('contador-consultas').textContent = `(${dados.length})`;
      lista.replaceChildren(...dados.map(cartaoConsulta));
    } catch (e) {
      document.getElementById('contador-consultas').textContent = '';
      lista.replaceChildren(el('p', { class: 'vazio' }, 'Não foi possível carregar as consultas.'));
      throw e;
    }
  }

  function cartaoConsulta(c) {
    const resultado = c.erro
      ? el('p', { class: 'vazio' }, `Erro: ${c.erro}`)
      : el('div', { class: 'tabela-rolagem' }, el('table', {},
          el('thead', {}, el('tr', {}, ...c.colunas.map((n) => el('th', {}, n)))),
          el('tbody', {}, ...(c.linhas.length
            ? c.linhas.map((l) => el('tr', {}, ...l.map((v) => el('td', {}, v ?? '—'))))
            : [linhaVazia(c.colunas.length, 'Nenhum resultado')]))));

    return el('details', { class: 'cartao consulta' },
      el('summary', {}, el('span', {class:'numero-consulta'}, String(c.id).padStart(2, '0')), el('strong', {}, c.titulo), el('span', { class: 'contador' }, c.linhas ? `${c.linhas.length} registros` : '')),
      el('p', { class: 'suave' }, c.pergunta),
      el('details', {}, el('summary', {}, 'Detalhes da consulta'), el('p', {class:'suave'}, c.conceitos), el('pre', { class: 'sql' }, c.sql)),
      resultado);
  }

  return { carregar };
})();

// =====================================================================
// ABAS
// =====================================================================

const recursos = (() => {
  let dados = [];
  let filtro = 'todos';
  let proximoDesenho = 0;
  const tipos = { views: 'View', procedures: 'Procedure', triggers: 'Trigger automático', functions: 'Função' };
  const titulos = {
    vw_voo_completo: 'Operações de voo',
    vw_passageiro_resumo: 'Histórico dos passageiros',
    atualizar_status_voo: 'Alterar status do voo',
    verificar_conexoes_passageiros: 'Conexões de passageiros',
    registrar_alteracao_status_voo: 'Histórico de alterações',
    verificar_manutencao_voo: 'Proteção de manutenção',
    calcular_duracao_voo: 'Duração do voo',
    calcular_taxa_ocupacao_voo: 'Ocupação da aeronave',
  };
  const caminhoAviao = 'M50 3 57 31 91 49 91 56 58 47 59 76 72 85 72 90 51 84 29 90 29 85 42 76 42 47 9 56 9 49 43 31Z';

  function aviaoSvg(preenchimento = 100, rotulo = 'Aeronave') {
    const ns = 'http://www.w3.org/2000/svg';
    const svg = document.createElementNS(ns, 'svg');
    const id = `aviao-ocupacao-${++proximoDesenho}`;
    const defs = document.createElementNS(ns, 'defs');
    const clip = document.createElementNS(ns, 'clipPath');
    const silhueta = document.createElementNS(ns, 'path');
    const base = document.createElementNS(ns, 'path');
    const cor = document.createElementNS(ns, 'rect');
    svg.setAttribute('viewBox', '0 0 100 100');
    svg.setAttribute('class', 'silhueta-aviao');
    svg.setAttribute('role', 'img');
    svg.setAttribute('aria-label', rotulo);
    clip.setAttribute('id', id);
    silhueta.setAttribute('d', caminhoAviao);
    clip.append(silhueta);
    defs.append(clip);
    base.setAttribute('d', caminhoAviao);
    base.setAttribute('class', 'aviao-base');
    const valor = Math.max(0, Math.min(100, Number(preenchimento) || 0));
    cor.setAttribute('x', '0');
    cor.setAttribute('y', String(100 - valor));
    cor.setAttribute('width', '100');
    cor.setAttribute('height', String(valor));
    cor.setAttribute('clip-path', `url(#${id})`);
    cor.setAttribute('class', 'aviao-preenchimento');
    svg.append(defs, base, cor);
    return svg;
  }

  function contextoVoo(voo) {
    if (!voo) return null;
    const origem = [voo.cidade_origem, voo.nome_origem].find(Boolean);
    const destino = [voo.cidade_destino, voo.nome_destino].find(Boolean);
    return el('div', { class: 'detalhe-voo' },
      el('strong', {}, `Voo ${voo.num_voo} · ${voo.status || '—'}`),
      el('span', { class: 'codigo-rota' }, `${voo.cod_origem} → ${voo.cod_destino}`),
      el('span', {}, `${origem || voo.cod_origem} → ${destino || voo.cod_destino}`),
      el('span', {}, `${fmtData(voo.data_voo)} · Saída ${fmtHora(voo.hora_partida)} · Chegada ${fmtHora(voo.hora_chegada)}`));
  }

  function tabela(r, trigger) {
    return el('div', { class: 'tabela-rolagem' }, el('table', {},
      el('thead', {}, el('tr', {}, ...r.colunas.map(c => el('th', {}, c)))),
      el('tbody', {}, ...(r.linhas.length ? r.linhas.map(l => el('tr', {}, ...l.map(v => el('td', {}, v ?? '—')))) :
        [linhaVazia(r.colunas.length, trigger ? 'Trigger não instalado ou não visível ao usuário conectado.' : 'Nenhum resultado.')]))));
  }

  function cartao(r) {
    const resultado = el('div', { class: 'resultado', 'aria-live': 'polite' });
    const form = el('form', { class: 'formulario' });
    if (r.nome === 'atualizar_status_voo' || r.tipo === 'functions') {
      form.append(el('label', {}, 'Número do voo', el('input', { name: 'voo', type: 'number', min: '1', required: '', placeholder: 'Informe o número' })));
    }
    if (r.nome === 'atualizar_status_voo') form.append(el('label', {}, 'Novo status', el('select', { name: 'status' },
      el('option', { value: 'Confirmado' }, 'Confirmado'), el('option', { value: 'Cancelado' }, 'Cancelado'))));
    const rotulosAcao = {
      vw_voo_completo: 'Abrir visão',
      vw_passageiro_resumo: 'Abrir visão',
      atualizar_status_voo: 'Atualizar status',
      verificar_conexoes_passageiros: 'Verificar conexões',
      registrar_alteracao_status_voo: 'Ver trigger',
      verificar_manutencao_voo: 'Ver trigger',
      calcular_duracao_voo: 'Calcular duração',
      calcular_taxa_ocupacao_voo: 'Calcular ocupação',
    };
    const botao = el('button', { type: 'submit', class: 'botao primario' }, rotulosAcao[r.nome] || 'Executar');
    const acoes = el('div', { class: 'acoes' }, botao);
    const fechar = el('button', {type:'button',class:'botao',hidden:'',onclick:() => {
      resultado.replaceChildren(); fechar.hidden = true;
      if (r.tipo === 'views') { botao.textContent = rotulosAcao[r.nome]; botao.setAttribute('aria-expanded','false'); }
    }}, 'Fechar resultado');
    acoes.append(fechar);
    if (r.tipo === 'views') botao.setAttribute('aria-expanded','false');
    async function executar(nome, valores, origem) {
      origem.disabled = true;
      resultado.replaceChildren(el('p', { class: 'suave' }, 'Consultando o banco…'));
      try {
        const resposta = await api('POST', '/api/recursos', { nome, ...valores });
        const dados = resposta.resultados[0];
        const primeiro = dados?.linhas?.[0];
        if (nome === 'calcular_duracao_voo' && primeiro) {
          const bruto = primeiro[dados.colunas.indexOf('duracao_minutos')];
          const minutos = bruto == null ? NaN : Number(bruto);
          resultado.replaceChildren(el('div', { class: 'resultado-funcao' },
            el('div', { class: 'leitura-funcao duracao' }, aviaoSvg(100, 'Símbolo de aeronave'),
              el('div', {}, el('strong', {}, Number.isFinite(minutos) ? `${Math.floor(minutos / 60)}h ${String(minutos % 60).padStart(2, '0')}min` : '—'),
                el('span', {}, Number.isFinite(minutos) ? `${minutos} min de voo` : 'Horários indisponíveis'))),
            contextoVoo(resposta.voo)));
        } else if (nome === 'calcular_taxa_ocupacao_voo' && primeiro) {
          const bruto = primeiro[dados.colunas.indexOf('ocupacao')];
          const cancelado = String(bruto).trim().toLowerCase() === 'cancelado';
          const valor = bruto == null ? NaN : Number(String(bruto).replace('%', '').trim().replace(',', '.'));
          const pct = Number.isFinite(valor) ? Math.max(0, Math.min(100, valor)) : 0;
          const voo = resposta.voo;
          const assentos = voo && voo.capacidade != null
            ? cancelado ? 'Voo cancelado' : `${voo.assentos_ocupados} de ${voo.capacidade} assentos` : 'Assentos ocupados';
          resultado.replaceChildren(el('div', { class: 'resultado-funcao' },
            el('div', { class: 'leitura-funcao ocupacao' }, aviaoSvg(pct, cancelado ? 'Voo cancelado' : `${Number.isFinite(valor) ? valor.toLocaleString('pt-BR') : 'Sem dados'} de ocupação`),
              el('div', {}, el('strong', {}, cancelado ? 'Cancelado' : Number.isFinite(valor) ? `${valor.toLocaleString('pt-BR')}%` : '—'),
                el('span', {}, cancelado ? 'Sem assentos contabilizados' : assentos))),
            contextoVoo(voo)));
        } else {
          const detalhes = nome === 'atualizar_status_voo' ? contextoVoo(resposta.voo) : null;
          resultado.replaceChildren(el('p', { class: 'suave' }, resposta.mensagem), ...(detalhes ? [detalhes] : []),
            ...resposta.resultados.map(t => tabela(t, r.tipo === 'triggers' && nome !== 'historico_status')));
        }
        fechar.hidden = false;
        if (r.tipo === 'views') { botao.textContent = 'Fechar visão'; botao.setAttribute('aria-expanded','true'); }
      } catch (e) {
        resultado.replaceChildren(el('p', { class: 'vazio' }, e.message));
      } finally { origem.disabled = false; }
    }
    form.addEventListener('submit', ev => {
      ev.preventDefault();
      if (r.tipo === 'views' && botao.getAttribute('aria-expanded') === 'true') { fechar.click(); return; }
      const valores = Object.fromEntries(new FormData(form));
      if (r.nome === 'atualizar_status_voo' && !confirm('Atualizar o voo ' + valores.voo + ' para ' + valores.status + '?')) return;
      executar(r.nome, valores, botao);
    });
    if (r.nome === 'registrar_alteracao_status_voo') {
      const historico = el('button', { type: 'button', class: 'botao' }, 'Ver histórico de status');
      historico.addEventListener('click', () => executar('historico_status', {}, historico));
      acoes.append(historico);
    }
    form.append(acoes);
    return el('article', { class: `cartao recurso recurso-${r.tipo}` },
      el('div', { class: 'recurso-cabecalho' }, el('h2', {}, titulos[r.nome] || r.nome), el('span', { class: 'selo' }, tipos[r.tipo])),
      el('details', {}, el('summary', {}, 'Definição SQL'), el('span', {class:'nome-sql'}, `${r.nome} · ${r.arquivo}`), el('pre', { class: 'sql' }, r.sql)),
      form, resultado);
  }
  function mostrar() {
    document.getElementById('lista-recursos').replaceChildren(...dados.filter(r => filtro === 'todos' || r.tipo === filtro).map(cartao));
  }
  document.querySelectorAll('[data-tipo]').forEach(b => b.addEventListener('click', () => {
    filtro = b.dataset.tipo;
    document.querySelectorAll('[data-tipo]').forEach(outro => {
      outro.classList.toggle('ativo', outro === b);
      outro.setAttribute('aria-pressed', outro === b);
    });
    mostrar();
  }));
  return { async carregar() {
    dados = await api('GET', '/api/recursos');
    for (const tipo of ['todos', 'views', 'procedures', 'triggers', 'functions']) {
      const quantidade = tipo === 'todos' ? dados.length : dados.filter(r => r.tipo === tipo).length;
      document.getElementById(`contagem-${tipo}`).textContent = quantidade;
    }
    mostrar();
  } };
})();

const carregadores = {
  passageiros: passageiros.carregar,
  voos: voos.carregar,
  dashboard: dashboard.carregar,
  consultas: consultas.carregar,
  recursos: recursos.carregar,
};
const paginas = {
  dashboard: 'Painel de operações',
  passageiros: 'Passageiros',
  voos: 'Voos',
  consultas: 'Consultas',
  recursos: 'Recursos do banco'
};
let abaAtual = 'dashboard';
let carregamento = 0;
async function atualizar() {
  const versao = ++carregamento;
  const estado = document.getElementById('estado-pagina');
  const botao = document.getElementById('atualizar');
  estado.hidden = false;
  estado.classList.remove('erro');
  estado.textContent = 'Carregando dados…';
  botao.disabled = true;
  try {
    await carregadores[abaAtual]();
    if (versao === carregamento) estado.hidden = true;
  } catch (e) {
    if (versao === carregamento) {
      estado.classList.add('erro');
      estado.textContent = e.message + ' Use Atualizar dados para tentar novamente.';
    }
  } finally { if (versao === carregamento) botao.disabled = false; }
}
function abrirAba(nome) {
  abaAtual = Object.hasOwn(paginas, nome) ? nome : 'dashboard';
  document.querySelectorAll('.aba').forEach(b => {
    const ativa = b.dataset.aba === abaAtual;
    b.classList.toggle('ativa', ativa);
    if (ativa) b.setAttribute('aria-current', 'page'); else b.removeAttribute('aria-current');
  });
  document.querySelectorAll('.painel').forEach(p => p.hidden = p.id !== 'aba-' + abaAtual);
  document.getElementById('titulo-pagina').textContent = paginas[abaAtual];
  history.replaceState(null, '', '#' + abaAtual);
  atualizar();
}
document.querySelectorAll('.aba').forEach(b => {
  const nome = [...b.childNodes].filter(n => n.nodeType === Node.TEXT_NODE).map(n => n.textContent).join('').trim();
  b.setAttribute('aria-label',nome);
  b.title = nome;
  for (const no of [...b.childNodes]) {
    if (no.nodeType === Node.TEXT_NODE && no.textContent.trim()) {
      const texto = el('span',{class:'texto-aba'},no.textContent.trim());
      no.replaceWith(texto);
    }
  }
  b.addEventListener('click', () => abrirAba(b.dataset.aba));
});
const controleMenu = document.getElementById('recolher-menu');
function recolherMenu(recolhido) {
  document.body.classList.toggle('menu-recolhido',recolhido);
  controleMenu.setAttribute('aria-expanded',String(!recolhido));
  controleMenu.setAttribute('aria-label',recolhido ? 'Expandir menu lateral' : 'Recolher menu lateral');
  controleMenu.title = recolhido ? 'Expandir menu' : 'Recolher menu';
}
controleMenu.addEventListener('click',() => {
  const recolhido = !document.body.classList.contains('menu-recolhido');
  recolherMenu(recolhido);
  try { localStorage.setItem('rec-menu-recolhido',String(recolhido)); } catch {}
});
try { recolherMenu(localStorage.getItem('rec-menu-recolhido') === 'true'); } catch {}
document.getElementById('atualizar').addEventListener('click', atualizar);
window.addEventListener('hashchange', () => {
  if (location.hash !== '#conteudo') abrirAba(location.hash.slice(1));
});
abrirAba(location.hash.slice(1));
