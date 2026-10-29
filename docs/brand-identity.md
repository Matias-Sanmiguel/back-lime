# Lime — Identidad de marca

Referencia histórica: **Editorial Lime**. La identidad vigente del frontend está documentada en `front-lime/DESIGN.md`, junto con sus tokens y componentes minimalistas. Este documento conserva la propuesta anterior, no dirige el rediseño actual.

No implementar pantallas de marketplace desde este documento; la preview de marca es el único artefacto de UI permitido acá.

---

## Concepto

Lime es un marketplace inmobiliario argentino, accesible y masivo. La marca se siente como si una página editorial de Notion y una app nativa de macOS hubieran diseñado un producto de propiedades — con una lima como firma, no como mascota.

Una línea: **páginas de tinta sobre papel, controles precisos, una lima que también es una planta.**

---

## Influencias (traducidas, no copiadas)

**Notion — ritmo, no skin**

- Contenido en bloques, lectura vertical, alineación izquierda.
- Densidad con aire: ni landing enorme ni vacío arbitrario.
- Papel cálido, tinta casi negra, grises de poco croma.
- Separar con espacio, luego con línea de 1 px, después con caja.
- Serif solo en títulos y wordmark. UI en sans.

**macOS — precisión, no disfraz**

- Grilla 8 px (ajustes ópticos de 4 px).
- Jerarquía por tamaño, peso, color y posición — no por sombras.
- Controles compactos (36–40 px), iconos 16/20/24, trazo constante.
- Claro y oscuro como dos superficies diseñadas, no una inversión.
- Una sola capa de blur, si hace falta, en navegación.
- Cero semáforos, ventanas, SF Symbols o glassmorphism decorativo.

**Inmobiliario + lima**

- El isotipo es una rodaja. Un gajo se vuelve vano (ventana / planta).
- El producto habla de m², barrios y avisos; la marca no dibuja casas ni pines.

---

## Personalidad

Editorial, precisa, accesible. Bold en la voz tipográfica, quieta en el chrome.

| Hacer | Evitar |
|-------|--------|
| Voseo corto: Publicá, Buscá, Consultá | Hype, “el mejor portal”, Learn more |
| Un acento lima por vista | Experiencia teñida de verde |
| Radios 6–10 px; pills solo en tags | Todo redondeado o flotando |
| Inter para datos y controles | Serif en labels, precios o nav |

---

## Logotipo

### Isotipo

Siete gajos de lima + **abertura superior rectangular** (ventana) + **patio cuadrado** al centro.

- Se lee lima de inmediato.
- El vano y el patio sugieren planta / abertura, sin casa, techo, llave, pin ni letra L.
- Óptica: el vano es más estrecho que un gajo radial puro, para no parecer porción de torta, sol, flor ni spinner.
- Fuente: [`docs/brand/lime-mark.svg`](brand/lime-mark.svg) — `currentColor`, sin dependencias.
- Lockup: [`docs/brand/lime-lockup.svg`](brand/lime-lockup.svg) — mark + wordmark Newsreader Semibold.

### Construcción y seguridad

- Lienzo 64×64. El círculo toca un margen interno de 1 px.
- Espacio de seguridad = **¼ del diámetro** (16 px en el lienzo de 64) alrededor del mark.
- No colocar texto ni iconos dentro de esa zona.

### Tamaños mínimos

| Uso | Tamaño | Nota |
|-----|--------|------|
| Favicon / tab | 16 px | El vano tiene que seguir leyéndose; no compactar el patio |
| Nav / lockup chico | 24–32 px | Default en chrome |
| Header de marca | 64 px | |
| Display / poster | 256 px | |

Si a 16 px el patio ensucia, usar solo gajos + vano (omití el cuadrado central). No hace falta un archivo extra hasta que el producto lo pida.

### Variantes

| Superficie | Color del mark |
|------------|----------------|
| Paper / White | Deep Lime `#326B2F` |
| Dark `#121212` / `#1C1C1E` | Lime `#B8F34B` |
| Lime `#B8F34B` | Ink `#1D1D1F` |
| Monocromo claro | Ink |
| Monocromo oscuro | Paper `#F4F4F1` |

Wordmark: **Lime** en Newsreader 600, tracking −0.03em. En claro: Ink. En oscuro: Dark text. Nunca Lime brillante a tamaño de UI.

---

## Tipografía

Dos familias, open source:

| Rol | Familia | Pesos | Uso |
|-----|---------|-------|-----|
| Display / wordmark | **Newsreader** | 500, 600 | Títulos de página, H1, marca |
| UI / cuerpo / datos | **Inter** | 400, 500, 600, 700 | Nav, labels, body, números, controles |

Newsreader: voz editorial contemporánea (cercana a la serif de Notion), no Didot/Bodoni ni inmobiliaria de lujo. Si un entorno no carga Newsreader, **Source Serif 4** 600 es el fallback documentado.

No usar fuentes propietarias de Notion ni de Apple. No imitar el wordmark de Notion.

### Escala (base 16 px, desktop)

| Token | Size | Line | Weight | Tracking | Familia |
|-------|------|------|--------|----------|---------|
| `display` | 40 px | 1.15 | 600 | −0.03em | Newsreader |
| `h1` | 32 px | 1.2 | 600 | −0.025em | Newsreader |
| `h2` | 22 px | 1.25 | 600 | −0.02em | Newsreader |
| `h3` | 16 px | 1.35 | 600 | −0.01em | Inter |
| `body` | 16 px | 1.5 | 400 | 0 | Inter |
| `small` | 14 px | 1.45 | 400 | 0 | Inter |
| `label` | 13 px | 1.3 | 500 | 0.01em | Inter |
| `caption` | 12 px | 1.35 | 500 | 0.02em | Inter |

Controles: Inter 500/600, 14–15 px. Precios: Inter 600, tabular nums si está disponible.

---

## Paleta

| Token | Hex | Uso |
|-------|-----|-----|
| `ink` | `#1D1D1F` | Estructura, CTA primario, texto |
| `paper` | `#F7F7F4` | Fondo editorial |
| `white` | `#FFFFFF` | Superficies puntuales, campos |
| `lime` | `#B8F34B` | Firma: mark, selección, highlights |
| `deep-lime` | `#326B2F` | Verde con contraste (mark claro, links) |
| `mist` | `#E8E8E3` | Líneas, chips quietos, hover |
| `muted` | `#6B6B67` | Meta, ayuda (no texto chico crítico) |
| `bg-dark` | `#121212` | Fondo oscuro |
| `surface-dark` | `#1C1C1E` | Superficie oscura |
| `text-dark` | `#F4F4F1` | Texto sobre oscuro |

Semánticos (uso puntual, no paleta de marca): danger `#C74646`, warning `#B25E09`.

**Balance:** 80–90 % neutros, 10–20 % lima.

### Contraste (WCAG AA)

| Par | Uso | AA texto normal |
|-----|-----|-----------------|
| Ink sobre Paper / White | Cuerpo, H1 | Sí |
| Deep Lime sobre White / Paper | Links, mark | Sí (no usar en caption 12 px si duda) |
| White sobre Ink | CTA | Sí |
| Text-dark sobre bg-dark | Cuerpo oscuro | Sí |
| Lime sobre bg-dark | Mark, highlight grande | Sí en ≥ 18 px / íconos |
| Lime sobre White como texto 14 px | — | **No.** Usar Deep Lime o Ink |
| Muted sobre Paper | Caption | Preferir 14 px+ |

CTA principal: **Ink + texto blanco**. Lime no carga texto.

---

## Tokens de interfaz

### Espaciado (8 px)

`4, 8, 12, 16, 24, 32, 48, 64`

### Radios

| Token | Valor | Uso |
|-------|-------|-----|
| `r-sm` | 6 px | Inputs, botones |
| `r-md` | 8 px | Superficies, bloques |
| `r-lg` | 10 px | Máximo de contenedor |
| `r-pill` | 999 px | Tags y estados, nada más |

### Bordes y elevación

- Hairline: 1 px `mist` (claro) / `#2C2C2E` (oscuro).
- Sombra: ninguna por defecto. Si una superficie está elevada: `0 1px 2px rgba(29,29,31,0.06)`.
- Blur: solo nav, `backdrop-filter: blur(12px)`, una capa.

### Iconografía

Lineal, 16 / 20 / 24 px, stroke 1.5, caps round, monocroma (`ink` / `text-dark`). Motivos: vano, planta, gajo, calle. No illustration pack, no stock corporativo, no infantil.

### Controles

- Altura desktop 36–40 px.
- Primario: fill Ink, texto White, radio 6.
- Secundario: borde 1 px, fondo transparente, visualmente silencioso.
- Campo: fondo White sobre Paper, borde mist, focus Deep Lime 1 px (no glow).

---

## Claro / oscuro

No invertir. Oscuro usa `bg-dark` y `surface-dark`; el lima sube a `#B8F34B` porque Deep Lime desaparece. El papel no se vuelve gris medio.

---

## Usos prohibidos

- Recolorear el mark a naranja, celeste o “portal 2015”.
- Estirar, rotar 15°, agregar sombra o gradiente al isotipo.
- Meter el mark dentro de un squircle de app genérico con padding de más.
- Wordmark en Inter Bold o en Lime brillante.
- Imitar la N de Notion, semáforos de macOS o un slice de loading.
- Tres propuestas o submarcas. Una dirección: Editorial Lime.

---

## Preview

Abrir [`docs/brand/preview.html`](brand/preview.html).

---

## Fuera de alcance de este doc

Backend, API, Docker, dominio inmobiliario, scaffold React. Cuando exista UI de producto, estos tokens son la ley; las pantallas se diseñan después.
