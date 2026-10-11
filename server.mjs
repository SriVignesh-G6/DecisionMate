import http from 'node:http'
import { URL } from 'node:url'

let nextId = 1
let nextCritId = 1
let nextAltId = 1

let decisions = [
  {
    id: nextId++,
    title: 'Choose a Laptop',
    description: 'Evaluate laptop models for college work and daily tasks.',
    createdAt: new Date().toISOString(),
    criteria: [
      { id: nextCritId++, name: 'Price', weight: 5 },
      { id: nextCritId++, name: 'Performance', weight: 4 },
      { id: nextCritId++, name: 'Battery Life', weight: 3 },
    ],
    alternatives: [
      { id: nextAltId++, name: 'Laptop A', description: 'Budget-friendly option with solid specs' },
      { id: nextAltId++, name: 'Laptop B', description: 'High-performance powerhouse' },
      { id: nextAltId++, name: 'Laptop C', description: 'Lightweight ultrabook with all-day battery' },
    ],
    scores: [
      { alternativeId: 1, criterionId: 1, scoreValue: 9 },
      { alternativeId: 1, criterionId: 2, scoreValue: 6 },
      { alternativeId: 1, criterionId: 3, scoreValue: 7 },
      { alternativeId: 2, criterionId: 1, scoreValue: 5 },
      { alternativeId: 2, criterionId: 2, scoreValue: 9 },
      { alternativeId: 2, criterionId: 3, scoreValue: 6 },
      { alternativeId: 3, criterionId: 1, scoreValue: 6 },
      { alternativeId: 3, criterionId: 2, scoreValue: 7 },
      { alternativeId: 3, criterionId: 3, scoreValue: 9 },
    ],
  },
]

function summary(d) {
  return {
    id: d.id,
    title: d.title,
    description: d.description,
    createdAt: d.createdAt,
    criteriaCount: d.criteria.length,
    alternativesCount: d.alternatives.length,
  }
}

function detail(d) {
  return {
    ...summary(d),
    criteria: d.criteria.map((c) => ({ id: c.id, name: c.name, weight: c.weight })),
    alternatives: d.alternatives.map((a) => ({ id: a.id, name: a.name, description: a.description })),
    scores: d.scores.map((s) => ({
      alternativeId: s.alternativeId,
      criterionId: s.criterionId,
      score: s.scoreValue,
    })),
  }
}

function calculateResults(d) {
  const cs = d.criteria
  const as = d.alternatives
  const scoreMap = new Map()
  d.scores.forEach((s) => scoreMap.set(`${s.alternativeId}:${s.criterionId}`, s.scoreValue))

  const totalWeight = cs.reduce((sum, c) => sum + c.weight, 0)
  const rows = as.map((a) => {
    let weighted = 0
    if (totalWeight > 0) {
      for (const c of cs) {
        const val = scoreMap.get(`${a.id}:${c.id}`)
        if (val !== undefined && val !== null) {
          weighted += val * c.weight
        }
      }
    }
    const normalized = totalWeight === 0 ? 0 : weighted / totalWeight
    const rounded = Math.round(normalized * 100) / 100
    return {
      alternativeId: a.id,
      alternativeName: a.name,
      weightedScore: rounded,
      rank: 0,
    }
  })

  rows.sort((a, b) => {
    if (b.weightedScore !== a.weightedScore) return b.weightedScore - a.weightedScore
    return a.alternativeName.localeCompare(b.alternativeName)
  })

  return rows.map((r, i) => ({
    alternativeId: r.alternativeId,
    alternativeName: r.alternativeName,
    weightedScore: r.weightedScore,
    rank: i + 1,
  }))
}

function parseBody(req) {
  return new Promise((resolve, reject) => {
    let body = ''
    req.on('data', (chunk) => (body += chunk))
    req.on('end', () => {
      try {
        resolve(body ? JSON.parse(body) : {})
      } catch (err) {
        reject(err)
      }
    })
    req.on('error', reject)
  })
}

const server = http.createServer(async (req, res) => {
  res.setHeader('Access-Control-Allow-Origin', '*')
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, OPTIONS')
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization')

  if (req.method === 'OPTIONS') {
    res.writeHead(204)
    res.end()
    return
  }

  const url = new URL(req.url, `http://${req.headers.host || 'localhost:8080'}`)
  const path = url.pathname
  const method = req.method

  const send = (status, data) => {
    if (status === 204) {
      res.writeHead(204)
      res.end()
      return
    }
    res.writeHead(status, { 'Content-Type': 'application/json' })
    res.end(JSON.stringify(data))
  }

  try {
    // GET /api/health
    if (method === 'GET' && path === '/api/health') {
      return send(200, { status: 'ok', application: 'DecisionMate API' })
    }

    // GET /api/decisions
    if (method === 'GET' && path === '/api/decisions') {
      return send(200, decisions.map(summary))
    }

    // POST /api/decisions
    if (method === 'POST' && path === '/api/decisions') {
      const body = await parseBody(req)
      if (!body.title || !body.title.trim()) {
        return send(400, { error: 'Title is required' })
      }
      const newD = {
        id: nextId++,
        title: body.title.trim(),
        description: body.description || '',
        createdAt: new Date().toISOString(),
        criteria: [],
        alternatives: [],
        scores: [],
      }
      decisions.push(newD)
      return send(201, summary(newD))
    }

    // Matches /api/decisions/:id/criteria
    const critMatch = path.match(/^\/api\/decisions\/(\d+)\/criteria$/)
    if (method === 'POST' && critMatch) {
      const d = decisions.find((x) => x.id === Number(critMatch[1]))
      if (!d) return send(404, { error: 'Decision not found' })
      const body = await parseBody(req)
      const newCrit = {
        id: nextCritId++,
        name: (body.name || '').trim(),
        weight: Number(body.weight) || 1,
      }
      d.criteria.push(newCrit)
      return send(201, newCrit)
    }

    // DELETE /api/criteria/:id
    const delCritMatch = path.match(/^\/api\/criteria\/(\d+)$/)
    if (method === 'DELETE' && delCritMatch) {
      const critId = Number(delCritMatch[1])
      for (const d of decisions) {
        d.criteria = d.criteria.filter((c) => c.id !== critId)
        d.scores = d.scores.filter((s) => s.criterionId !== critId)
      }
      return send(204)
    }

    // Matches /api/decisions/:id/alternatives
    const altMatch = path.match(/^\/api\/decisions\/(\d+)\/alternatives$/)
    if (method === 'POST' && altMatch) {
      const d = decisions.find((x) => x.id === Number(altMatch[1]))
      if (!d) return send(404, { error: 'Decision not found' })
      const body = await parseBody(req)
      const newAlt = {
        id: nextAltId++,
        name: (body.name || '').trim(),
        description: body.description || '',
      }
      d.alternatives.push(newAlt)
      return send(201, newAlt)
    }

    // DELETE /api/alternatives/:id
    const delAltMatch = path.match(/^\/api\/alternatives\/(\d+)$/)
    if (method === 'DELETE' && delAltMatch) {
      const altId = Number(delAltMatch[1])
      for (const d of decisions) {
        d.alternatives = d.alternatives.filter((a) => a.id !== altId)
        d.scores = d.scores.filter((s) => s.alternativeId !== altId)
      }
      return send(204)
    }

    // PUT /api/decisions/:decisionId/scores
    const scoreMatch = path.match(/^\/api\/decisions\/(\d+)\/scores$/)
    if (method === 'PUT' && scoreMatch) {
      const d = decisions.find((x) => x.id === Number(scoreMatch[1]))
      if (!d) return send(404, { error: 'Decision not found' })
      const body = await parseBody(req)
      const alternativeId = Number(body.alternativeId)
      const criterionId = Number(body.criterionId)
      const scoreVal = Number(body.score)

      let scoreObj = d.scores.find(
        (s) => s.alternativeId === alternativeId && s.criterionId === criterionId
      )
      if (scoreObj) {
        scoreObj.scoreValue = scoreVal
      } else {
        scoreObj = { alternativeId, criterionId, scoreValue: scoreVal }
        d.scores.push(scoreObj)
      }
      return send(200, {
        alternativeId,
        criterionId,
        score: scoreObj.scoreValue,
      })
    }

    // GET /api/decisions/:id/results
    const resMatch = path.match(/^\/api\/decisions\/(\d+)\/results$/)
    if (method === 'GET' && resMatch) {
      const d = decisions.find((x) => x.id === Number(resMatch[1]))
      if (!d) return send(404, { error: 'Decision not found' })
      return send(200, calculateResults(d))
    }

    // GET /api/decisions/:id
    const decMatch = path.match(/^\/api\/decisions\/(\d+)$/)
    if (method === 'GET' && decMatch) {
      const d = decisions.find((x) => x.id === Number(decMatch[1]))
      if (!d) return send(404, { error: 'Decision not found' })
      return send(200, detail(d))
    }

    // PUT /api/decisions/:id
    if (method === 'PUT' && decMatch) {
      const d = decisions.find((x) => x.id === Number(decMatch[1]))
      if (!d) return send(404, { error: 'Decision not found' })
      const body = await parseBody(req)
      d.title = body.title.trim()
      d.description = body.description || ''
      return send(200, summary(d))
    }

    // DELETE /api/decisions/:id
    if (method === 'DELETE' && decMatch) {
      const id = Number(decMatch[1])
      decisions = decisions.filter((x) => x.id !== id)
      return send(204)
    }

    return send(404, { error: 'Endpoint not found' })
  } catch (err) {
    console.error(err)
    return send(500, { error: err.message || 'Internal server error' })
  }
})

const PORT = process.env.PORT || 8080
server.listen(PORT, () => {
  console.log(`DecisionMate API server running at http://localhost:${PORT}/api`)
})
