// =====================================================================
// Interface do Aeroporto — CRUD de Passageiros e Voos
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

  const modoNovo = () => {
    emEdicao = null;
    form.reset();
    form.elements[chave].readOnly = false;
    form.classList.remove('editando');
    titulo.textContent = textoNovo;
  };

  form.querySelector('[data-cancelar]').addEventListener('click', modoNovo);

  form.addEventListener('submit', async (ev) => {
    ev.preventDefault();
    const dados = Object.fromEntries(new FormData(form));
    try {
      const r = await aoSalvar(dados, emEdicao);
      avisar(r.mensagem);
      modoNovo();
    } catch (e) {
      avisar(e.message, true);
    }
  });

  return {
    modoNovo,
    editar(valores, id) {
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
      await carregar();
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
      tbody.replaceChildren(linhaVazia(7, e.message));
      return;
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
      tbody.replaceChildren(linhaVazia(10, e.message));
      return;
    }
    document.getElementById('contador-voos').textContent = `(${lista.length})`;
    if (!lista.length) {
      tbody.replaceChildren(linhaVazia(10, 'Nenhum voo cadastrado.'));
      return;
    }
    tbody.replaceChildren(...lista.map((v) => el('tr', {},
      el('td', { class: 'num' }, v.num_voo),
      el('td', {}, fmtData(v.data_voo)),
      el('td', {}, `${fmtHora(v.hora_partida)} → ${fmtHora(v.hora_chegada)}`),
      el('td', {}, `${v.origem} → ${v.destino}`),
      el('td', {}, v.cod_aeronave, el('div', { class: 'suave' }, `${v.modelo} · ${v.companhia}`)),
      el('td', {}, `${v.num_portao} / T${v.num_terminal}`),
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

  const moeda = (v) => Number(v).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
  const paleta = ['#0b5cad', '#e08a1e', '#1e8449', '#c0392b', '#7d3c98', '#17a2b8', '#8d6e63'];

  function desenhar(id, tipo, rotulos, valores, { legenda = false, cor = null, opcoes = {} } = {}) {
    graficos[id]?.destroy();
    const cores = cor ?? (tipo === 'doughnut' ? paleta : paleta[0]);
    graficos[id] = new Chart(document.getElementById(id), {
      type: tipo,
      data: { labels: rotulos, datasets: [{ data: valores, backgroundColor: cores, borderColor: cores, borderWidth: tipo === 'line' ? 2 : 0, tension: .25 }] },
      options: { maintainAspectRatio: false, plugins: { legend: { display: legenda } }, ...opcoes },
    });
  }

  function cartao(rotulo, valor) {
    return el('div', { class: 'indicador' }, el('span', { class: 'valor' }, valor), el('span', { class: 'rotulo' }, rotulo));
  }

  async function carregar() {
    try {
      const d = await api('GET', '/api/dashboard');
      Chart.defaults.color = getComputedStyle(document.body).getPropertyValue('--texto-suave') || '#5d6b7c';

      // indicadores (cards)
      const i = d.indicadores;
      document.getElementById('indicadores').replaceChildren(
        cartao('Voos', i.voos), cartao('Passageiros', i.passageiros),
        cartao('Aeronaves', i.aeronaves), cartao('Receita total', moeda(i.receita)));

      // gráficos de séries
      desenhar('grafico-status', 'doughnut', d.voosPorStatus.rotulos, d.voosPorStatus.valores, { legenda: true });
      desenhar('grafico-receita', 'bar', d.receitaPorCompanhia.rotulos, d.receitaPorCompanhia.valores);
      desenhar('grafico-dia', 'line', d.voosPorDia.rotulos, d.voosPorDia.valores,
               { opcoes: { scales: { y: { beginAtZero: true, ticks: { precision: 0 } } } } });
      desenhar('grafico-ocupacao', 'bar', d.ocupacaoPorVoo.rotulos, d.ocupacaoPorVoo.valores,
               { cor: paleta[1], opcoes: { scales: { y: { beginAtZero: true } } } });

      // estatística descritiva + histograma do preço
      const e = d.estatisticasPreco;
      document.getElementById('estatisticas').replaceChildren(
        cartao('n (bilhetes)', e.n), cartao('Média', moeda(e.media ?? 0)), cartao('Mediana', moeda(e.mediana ?? 0)),
        cartao('Desvio padrão', moeda(e.desvioPadrao ?? 0)), cartao('Mínimo', moeda(e.minimo ?? 0)), cartao('Máximo', moeda(e.maximo ?? 0)));
      desenhar('grafico-histograma', 'bar', d.histogramaPreco.rotulos, d.histogramaPreco.valores,
               { cor: paleta[2], opcoes: { scales: { y: { beginAtZero: true, ticks: { precision: 0 }, title: { display: true, text: 'Frequência' } },
                                                    x: { title: { display: true, text: 'Faixa de preço' } } } } });
    } catch (e) {
      avisar(e.message, true);
    }
  }

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
      avisar(e.message, true);
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

    return el('div', { class: 'cartao consulta' },
      el('h2', {}, `${c.id}. ${c.titulo} `, el('span', { class: 'contador' }, c.linhas ? `${c.linhas.length} linha(s)` : '')),
      el('p', { class: 'suave' }, c.pergunta),
      el('p', {}, el('span', { class: 'selo' }, 'Conceitos'), ` ${c.conceitos}`),
      el('details', {}, el('summary', {}, 'Ver SQL'), el('pre', { class: 'sql' }, c.sql)),
      resultado);
  }

  return { carregar };
})();

// =====================================================================
// ABAS
// =====================================================================

const carregadores = {
  passageiros: passageiros.carregar,
  voos: voos.carregar,
  dashboard: dashboard.carregar,
  consultas: consultas.carregar,
};

function abrirAba(nome) {
  const botao = document.querySelector(`.aba[data-aba="${nome}"]`) || document.querySelector('.aba');
  document.querySelectorAll('.aba').forEach((b) => b.classList.toggle('ativa', b === botao));
  document.querySelectorAll('.painel').forEach((p) => (p.hidden = p.id !== `aba-${botao.dataset.aba}`));
  history.replaceState(null, '', `#${botao.dataset.aba}`);
  carregadores[botao.dataset.aba]?.();
}

document.querySelectorAll('.aba').forEach((botao) => {
  botao.addEventListener('click', () => abrirAba(botao.dataset.aba));
});

abrirAba(location.hash.slice(1)); // a aba fica na URL (#voos) e sobrevive ao F5
