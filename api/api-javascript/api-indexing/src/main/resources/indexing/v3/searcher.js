/*
 * Copyright (c) 2010-2021 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: 2010-2021 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
var java = require('core/v3/java');

exports.search = function(index, term) {
	var results = java.call('org.eclipse.dirigible.api.v3.indexing.IndexingFacade', 'search', [index, term]);
	return JSON.parse(results);
};

exports.before = function(index, date) {
	var results = java.call('org.eclipse.dirigible.api.v3.indexing.IndexingFacade', 'before', [index, '' + date.getTime()]);
	return JSON.parse(results);
};

exports.after = function(index, date) {
	var results = java.call('org.eclipse.dirigible.api.v3.indexing.IndexingFacade', 'after', [index, '' + date.getTime()]);
	return JSON.parse(results);
};

exports.between = function(index, lower, upper) {
	var results = java.call('org.eclipse.dirigible.api.v3.indexing.IndexingFacade', 'between', [index, '' + lower.getTime(), '' + upper.getTime()]);
	return JSON.parse(results);
};
