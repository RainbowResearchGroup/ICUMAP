"use strict";

let identity = x => x;

Array.prototype.average = function() {
    return this.sum() / this.length;
};

Array.prototype.count = function(f) {
    return this.filter(f).length;
};

Array.prototype.distinct = function() {
    return this.distinctBy(identity);
};

Array.prototype.distinctBy = function(f) {
    return this.groupBy(f, l => l[0]).map(p => p[1]);
};

Array.prototype.drop = function(n) {
    return this.filter((_, i) => i >= n)
};

Array.prototype.dropRight = function(n) {
    return this.reverse().drop(n).reverse()
};

Array.prototype.includesWhere = function(f) {
    return this.count(f) > 0;
};

Array.prototype.indices = function() {
    return this.map((_, i) => i);
};

Array.prototype.indexWhere = function(f, v) {
    return this.map(f).indexOf(v)
};

Array.prototype.find = function(f) {
    for (let i = 0; i < this.length; i++) {
        if (f(this[i])) return this[i];
    }
    return null;
};

Array.prototype.flatMap = function(f, depth = Number.MAX_SAFE_INTEGER) {
    return this.map(f).flatten(depth)
};

Array.prototype.flatten = function(depth = Number.MAX_SAFE_INTEGER) {
    let k = [];
    function f(l, d) {
        if (d === 0 || typeof l !== 'object') {
            k.push(l);
        } else {
            l.forEach(e => f(e, d - 1));
        }
    }
    f(this, depth);
    return k;
};

Array.prototype.groupBy = function(f, g = identity) {
    let dict = {};
    this.forEach(x => f(x) in dict ? dict[f(x)].push(x) : dict[f(x)] = [x]);
    let out = [];
    for (let k in dict) out.push([k, g(dict[k])]);
    return out;
};

Array.prototype.max = function() {
    return this.length === 0 ? undefined : this.reduce((a, b) => Math.max(a, b))
};

Array.prototype.min = function() {
    return this.length === 0 ? undefined : this.reduce((a, b) => Math.min(a, b));
};

Array.prototype.sliding = function(size, step = size) {
    let output = [];
    for (let start = 0; start < this.length; start += step) {
        output.push(this.slice(start, start + size));
    }
    return output;
};

Array.prototype.sortBy = function(f) {
    return this.sort((a, b) => f(a) - f(b))
};

Array.prototype.sortNumeric = function() {
    return this.sort((a, b) => a - b);
};

Array.prototype.sortWith = Array.prototype.sort;

Array.prototype.sum = function() {
    return this.reduce((a, b) => a + b, 0)
};

Array.prototype.take = function(n) {
    return this.slice(0, n);
};

Array.prototype.takeRight = function (n) {
    return this.reverse().take(n).reverse();
};

Array.prototype.toDict = function() {
    let dict = {};
    this.forEach(p => dict[p[0]] = p[1]);
    return dict;
};

Array.prototype.zipWith = function(b) {
    return this.map((x, i) => [x, b[i]])
};

Array.prototype.zipWithIndices = function() {
    return this.zipWith(this.indices())
};

Array.prototype.zipWithIndex = Array.prototype.zipWithIndices;

Array.prototype.effect = function(f) {
    f(this);
    return this;
};